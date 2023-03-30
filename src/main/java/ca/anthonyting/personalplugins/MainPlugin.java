package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.commands.*;
import ca.anthonyting.personalplugins.listeners.*;
import ca.anthonyting.personalplugins.tabcomplete.EmojiMessageComplete;
import ca.anthonyting.personalplugins.tabcomplete.GetStatComplete;
import io.github.radbuilder.emojichat.EmojiChat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class MainPlugin extends JavaPlugin {
    
    private static MainPlugin instance;
    public static MainPlugin getInstance() {
        return instance;
    }
    private ServerListListener serverListListener = null;
    private TempBackup backupMaker;
    private LinkedHashMap<String, Character> emojis;

    private void registerListeners() {
        if (getConfig().getBoolean("disable-player-head-drop")) {
            getLogger().info("Player Head Drop disabled.");
        } else {
            getServer().getPluginManager().registerEvents(new PlayerHeadListener(), this);
        }

        if (!GraphicsEnvironment.isHeadless()) {
            if (getConfig().getBoolean("disable-console-rename")) {
                getLogger().info("Console Rename disabled.");
            } else {
                serverListListener = new ServerListListener();
                getServer().getPluginManager().registerEvents(serverListListener, this);
            }
        }

        if (Bukkit.getVersion().contains("1.15")) {
            if (getConfig().getBoolean("allow-donkey-dupe")) {
                getLogger().info("Donkey Dupe allowed.");
            } else {
                getServer().getPluginManager().registerEvents(new ItemDupeListener(), this);
            }
        }

        if (getConfig().getBoolean("allow-many-spawner-mobs")) {
            getLogger().info("Allow spawners to generate many mobs in a region.");
        } else {
            getServer().getPluginManager().registerEvents(new MobSpawnerListener(), this);
        }

        if (getConfig().getBoolean("cow-sacrifice")) {
            getServer().getPluginManager().registerEvents(new CowSacrificeListener(), this);
        } else {
            getLogger().info("Cow Sacrifice disabled.");
        }
    }

    private void initializeBackups() {
        String backupDirectoryName = getConfig().getString("temp-backup-directory");
        long delay = getConfig().getLong("backup-freq");
        if (backupDirectoryName == null || delay < 1) {
            getLogger().info("Backups disabled.");
        } else {
            getServer().getPluginManager().registerEvents(new PlayerCountListener(), this);
            Path backupPath = Paths.get(backupDirectoryName);
            var task = new BukkitRunnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        String backupDirectoryName = getConfig().getString("temp-backup-directory");
                        if (backupDirectoryName != null) {
                            backupMaker.setBackupPath(Paths.get(backupDirectoryName));
                        }
                        if (backupMaker.havePlayersBeenOffline()) {
                            backupMaker.setReady(true);
                            this.notifyAll();
                            return;
                        }
                        getLogger().info("Saving worlds and players...");
                        MainPlugin.getInstance().getServer().savePlayers();
                        MainPlugin.getInstance().getServer().getWorlds().forEach(World::save);
                        backupMaker.setReady(true);
                        this.notifyAll();
                    }
                }
            }.runTaskTimer(this, delay*20, delay*20);
            backupMaker = new TempBackup(backupPath, delay, task);
            backupMaker.runTaskTimerAsynchronously(this, delay*20, delay*20);
        }

        PluginCommand forceBackup = getCommand("forcebackup");
        if (forceBackup != null) {
            forceBackup.setExecutor(new Backup());
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        registerListeners();
        initializeBackups();

        PluginCommand playtime = getCommand("playtime");
        if (playtime != null) {
            playtime.setExecutor(new PlayTime());
        }

        PluginCommand getStat = getCommand("getstat");
        if (getStat != null) {
            getStat.setExecutor(new GetStat());
            getStat.setTabCompleter(new GetStatComplete());
        }

        var emojichat = (EmojiChat) Bukkit.getPluginManager().getPlugin("EmojiChat");
        PluginCommand emoji = getCommand("emoji");
        if (emojichat != null && emoji != null) {
            var emojis = emojichat.getEmojiHandler().getEmojis();
            emoji.setExecutor(new EmojiMessage());
            emoji.setTabCompleter(new EmojiMessageComplete());
            this.emojis = emojis;
        }

        var stopServer = getCommand("stopserver");
        if (stopServer != null) {
            stopServer.setExecutor(new StopServer());
        }
    }

    @Override
    public void onDisable() {
        if (serverListListener != null) {
            if (serverListListener.setTitle(serverListListener.getOrigTitle())) {
                getLogger().info("Resetting title to original title...");
            } else {
                getLogger().warning("Failed to reset title to original title!");
            }
        }
        getServer().getScheduler().cancelTasks(this);
        instance = null;
    }

    public TempBackup getBackupMaker() {
        return backupMaker;
    }

    public LinkedHashMap<String, Character> getEmojis() {
        return this.emojis;
    }
}