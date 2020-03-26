package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.commands.Backup;
import ca.anthonyting.personalplugins.listeners.MobSpawnerListener;
import ca.anthonyting.personalplugins.listeners.PlayerHeadListener;
import ca.anthonyting.personalplugins.listeners.ServerListListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends JavaPlugin {
    
    private static Main instance;
    public static Main getPlugin() {
        return instance;
    }
    private ServerListListener serverListListener = null;
    private static TempBackup backupMaker;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (getConfig().getBoolean("disable-player-head-drop")) {
            getLogger().info("Player Head Drop disabled.");
        } else {
            getServer().getPluginManager().registerEvents(new PlayerHeadListener(), this);
        }

        if (getConfig().getBoolean("disable-console-rename")) {
            getLogger().info("Console Rename disabled.");
        } else {
            serverListListener = new ServerListListener();
            getServer().getPluginManager().registerEvents(serverListListener, this);
        }

        getServer().getPluginManager().registerEvents(new MobSpawnerListener(), this);

        String backupDirectoryName = getConfig().getString("temp-backup-directory");
        long delay = getConfig().getLong("backup-freq");
        if (backupDirectoryName == null || delay < 1) {
            getLogger().info("Backups disabled.");
        } else {
            Path backupPath = Paths.get(backupDirectoryName);
            backupMaker = new TempBackup(TempBackup.getWorldDirectories(), backupPath);
            backupMaker.runTaskTimer(this, delay*20, delay*20);
        }

        getCommand("tempbackup").setExecutor(new Backup());
    }

    @Override
    public void onDisable() {
        if (serverListListener == null) {
            return;
        }
        if (serverListListener.setTitle(serverListListener.getOrigTitle())) {
            getLogger().info("Resetting title to original title...");
        } else {
            getLogger().warning("Failed to reset title to original title!");
        }
        instance = null;
        getServer().getScheduler().cancelTasks(this);
    }

    public static TempBackup getBackupMaker() {
        return backupMaker;
    }
}