package ca.anthonyting.personalplugins;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    
    private static Main instance;
    public static Main getPlugin() {
        return instance;
    }
    private ServerListListener serverListListener;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new PlayerHeadListener(), this);

        serverListListener = new ServerListListener();
        getServer().getPluginManager().registerEvents(serverListListener, this);
    }

    @Override
    public void onDisable() {
        if (serverListListener.setTitle(serverListListener.getOrigTitle())) {
            getLogger().info("Resetting title to original title...");
        } else {
            getLogger().warning("Failed to reset title to original title!");
        }
    }
}