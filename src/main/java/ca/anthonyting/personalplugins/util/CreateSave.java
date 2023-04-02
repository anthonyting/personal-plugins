package ca.anthonyting.personalplugins.util;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class CreateSave implements AutoCloseable {
    private final MainPlugin main = MainPlugin.getInstance();

    public CreateSave(BukkitRunnable parent) throws InterruptedException {
        new SaveRunnable(parent).runTask(main);
        parent.wait();
    }

    @Override
    public void close() {
        var server = main.getServer();
        server.getWorlds().forEach(f -> f.setAutoSave(true));
    }
}
