package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.remnant.RemnantManager;
import ca.anthonyting.personalplugins.util.PlayerDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.entity.Item;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class RemnantDeathListener implements Listener {

    private final MainPlugin plugin;
    private final RemnantManager remnants;

    public RemnantDeathListener(RemnantManager remnants) {
        this.remnants = remnants;
        this.plugin = remnants.getPlugin();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRemnantDropDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item) || !isRemnantDrop(item)) {
            return;
        }

        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> {
                event.setCancelled(true);
                item.setFireTicks(0);
            }
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRemnantDropCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item item) || !isRemnantDrop(item)) {
            return;
        }

        event.setCancelled(true);
        item.setFireTicks(0);
    }

    private boolean isRemnantDrop(Item item) {
        return item.getPersistentDataContainer().has(remnants.getDropKey(), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRemnantDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mannequin remnant)
                || !remnants.isRemnant(remnant)) {
            return;
        }

        remnant.setSilent(true);
        remnants.clearRemnant(remnant);
        notifyOwnerAndRemoveCompass(remnant);
        UUID ownerUuid = getOwnerUuid(remnant);
        YamlConfiguration config = ownerUuid == null ? null : remnants.removeCachedPlayerData(ownerUuid);

        List<ItemStack> drops;
        if (config == null) {
            plugin.getLogger().warning("Player remnant died without cached player data; preserving its default item drops.");
            drops = event.getDrops().stream().map(ItemStack::clone).toList();
        } else {
            drops = PlayerDataHandler.getRemnantDrops(config);
            event.setDroppedExp(PlayerDataHandler.getRemnantExperienceDrop(config));
        }

        event.getDrops().clear();
        if (ownerUuid == null) {
            event.setDroppedExp(0);
            plugin.getLogger().warning("Player remnant has no valid owner UUID; suppressing drops to prevent public pickup.");
            return;
        }
        int spawnedDrops = 0;
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) {
                continue;
            }
            Item item = remnant.getWorld().dropItemNaturally(remnant.getLocation(), drop);
            item.setUnlimitedLifetime(false);
            item.setWillAge(true);
            item.setCanMobPickup(false);
            item.setOwner(ownerUuid);
            item.getPersistentDataContainer().set(remnants.getDropKey(), PersistentDataType.BYTE, (byte) 1);
            item.setFireTicks(0);
            spawnedDrops++;
        }
        plugin.getLogger().info("Player remnant died; spawned " + spawnedDrops
                + " owner-restricted item stacks with normal despawn behavior.");
    }

    private UUID getOwnerUuid(Mannequin remnant) {
        String ownerId = remnant.getPersistentDataContainer().get(
                remnants.getRemnantOwnerKey(),
                PersistentDataType.STRING
        );
        if (ownerId == null) {
            return null;
        }
        try {
            return UUID.fromString(ownerId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void notifyOwnerAndRemoveCompass(Mannequin remnant) {
        String ownerId = remnant.getPersistentDataContainer().get(
                remnants.getRemnantOwnerKey(),
                PersistentDataType.STRING
        );
        if (ownerId == null) {
            plugin.getLogger().warning("Remnant died without an owner UUID; compass cleanup was skipped.");
            return;
        }

        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerId);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Remnant died with an invalid owner UUID; compass cleanup was skipped.");
            return;
        }

        Location effectLocation = remnant.getLocation().add(0, 1, 0);
        remnant.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, effectLocation, 24, 0.45, 0.65, 0.45, 0.05);
        remnant.getWorld().playSound(effectLocation, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.8f);

        Player owner = Bukkit.getPlayer(ownerUuid);
        String instanceId = remnant.getPersistentDataContainer().get(
                remnants.getRemnantInstanceKey(),
                PersistentDataType.STRING
        );
        if (instanceId == null) {
            plugin.getLogger().warning("Remnant died without an instance ID; compass cleanup was skipped.");
            return;
        }

        if (owner == null) {
            remnants.queueCompassCleanup(ownerId, instanceId);
            plugin.getLogger().info("Queued compass cleanup for remnant " + instanceId + " and offline owner " + ownerId + ".");
            return;
        }

        int removedCompasses = removeMatchingCompass(owner.getInventory(), instanceId);
        if (isCompassFor(owner.getItemOnCursor(), instanceId)) {
            owner.setItemOnCursor(null);
            removedCompasses++;
        }
        if (removedCompasses > 0) {
            owner.saveData();
        }
        owner.sendMessage(ChatColor.GREEN + "Your remnant has fallen. Its compass fades away.");
        plugin.getLogger().info("Remnant for " + owner.getName() + " died; removed " + removedCompasses
                + " matching compass(es), played the success effect, and notified the owner.");
    }

    private int removeMatchingCompass(PlayerInventory inventory, String instanceId) {
        ItemStack[] contents = inventory.getContents();
        int removed = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!isCompassFor(item, instanceId)) {
                continue;
            }
            inventory.setItem(slot, null);
            removed++;
        }
        return removed;
    }

    private boolean isCompassFor(ItemStack item, String instanceId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String targetId = item.getItemMeta().getPersistentDataContainer().get(
                remnants.getCompassInstanceKey(),
                PersistentDataType.STRING
        );
        return instanceId.equals(targetId);
    }

}
