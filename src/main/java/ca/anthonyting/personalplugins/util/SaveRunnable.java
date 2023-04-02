package ca.anthonyting.personalplugins.util;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class SaveRunnable extends BukkitRunnable {
    private final MainPlugin main = MainPlugin.getInstance();
    private final BukkitRunnable parent;

    public SaveRunnable(BukkitRunnable parent) {
        this.parent = parent;
    }

    /**
     * Must disable auto-save before saving worlds to prevent inconsistent state.
     */
    private void saveWorlds() {
        main.getLogger().info("Saving worlds and players...");
        var server = main.getServer();
        server.getWorlds().forEach(f -> f.setAutoSave(false));
        server.savePlayers();
        server.getWorlds().forEach(World::save);
    }

    @Override
    public void run() {
        saveWorlds();
        synchronized (parent) {
            parent.notifyAll();
        }
    }
}
