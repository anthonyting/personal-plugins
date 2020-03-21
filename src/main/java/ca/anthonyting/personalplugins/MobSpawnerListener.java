package ca.anthonyting.personalplugins;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;

public class MobSpawnerListener implements Listener {

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        EntityType thisEntityType = e.getEntityType();
        List<Entity> nearbyEntities = e.getEntity().getNearbyEntities(30, 30, 30);
        int count = 0;
        for (Entity entity : nearbyEntities) {
            if (entity.getType() == thisEntityType) {
                count++;
            }
        }
        if (count > 100) {
            e.setCancelled(true);
        }
    }
}
