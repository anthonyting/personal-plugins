package ca.anthonyting.personalplugins;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    
    private static Main instance;
    public static Main getPlugin() {
        return instance;
    }
    private ServerListListener serverListListener = null;

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
    }
}