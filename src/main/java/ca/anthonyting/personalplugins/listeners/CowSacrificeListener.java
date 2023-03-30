package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.util.TwentyBlockRegion;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

abstract class ClearCowDeathCountTask extends BukkitRunnable {
    private long cowDeathCount = 0;
    public long increment() {
        return ++cowDeathCount;
    }
}


public class CowSacrificeListener implements Listener {
    private final HashMap<TwentyBlockRegion, ClearCowDeathCountTask> cowDeathCountByRegion = new HashMap<>();
    private final MainPlugin plugin = MainPlugin.getInstance();

    @EventHandler
    public void onCowDeath(EntityDeathEvent e) {
        final var entity = e.getEntity();
        if (entity.getType() != EntityType.COW) {
            return;
        }

        var lastDamageCause = entity.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent)) {
            return;
        }

        final var damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
        if (damager.getType() != EntityType.PLAYER) {
            return;
        }

        final var cow = (Cow) entity;
        final var world = cow.getWorld();

        if (!world.getEnvironment().equals(World.Environment.NORMAL)) {
            return;
        }

        final var location = cow.getLocation();
        final var ritualCowCount = 150;
        final var region = new TwentyBlockRegion(location);
        final var task = cowDeathCountByRegion.get(region);
        if (task == null || task.isCancelled()) {
            var newTask = new ClearCowDeathCountTask() {
                @Override
                public void run() {
                    cowDeathCountByRegion.remove(region);
                }
            };
            cowDeathCountByRegion.put(region, newTask);
            // 1 minute in ticks
            final var delay = 20 * 60;
            newTask.runTaskLaterAsynchronously(plugin, delay);
        } else {
            final var count = task.increment();
            if (count >= ritualCowCount) {
                cowDeathCountByRegion.get(region).cancel();
                cowDeathCountByRegion.remove(region);
                world.strikeLightningEffect(location);
                world.setStorm(true);
                world.setThundering(true);
                // one minecraft day
                final var weatherDuration = 24000;
                world.setWeatherDuration(weatherDuration);
                world.setThunderDuration(weatherDuration);
                plugin.getServer().broadcastMessage(ChatColor.GOLD + "A storm has been summoned by " + damager.getName() + "!");
            }
        }
    }
}
