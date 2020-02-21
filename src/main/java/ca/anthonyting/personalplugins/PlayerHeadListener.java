package ca.anthonyting.personalplugins;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creeper;
import org.bukkit.World;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;

public class PlayerHeadListener implements Listener {

    private static ItemStack makeHead(Player player) {
        // returns null if setting the player of the skull fails
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = head.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) headMeta;
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
            head.setItemMeta(skullMeta);
            return head;
        }
        return null;
    }

    private static Creeper chargedCreeperThatKilledEntity(Entity entity) {
        // returns null if a charged creeper did not kill the entity
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) entity.getLastDamageCause();
            if (damageEvent.getDamager() instanceof Creeper) {
                Creeper creeperThatKilledEntity = (Creeper) damageEvent.getDamager();
                if (creeperThatKilledEntity.isPowered()) {
                    return creeperThatKilledEntity;
                }
            }
        }
        return null;
    }

    private Creeper previousCreeper = null;
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // drop a player head if player killed by charged creeper
        Player player = event.getEntity();
        World currentWorld = player.getWorld();
        if (currentWorld.getGameRuleValue(GameRule.DO_MOB_LOOT)) {
            Creeper creeperThatKilledPlayer = chargedCreeperThatKilledEntity(player);
            if (creeperThatKilledPlayer != null) {
                if (creeperThatKilledPlayer == previousCreeper) {
                    // only drop one head if multiple people are standing by
                    return;
                }
                previousCreeper = creeperThatKilledPlayer;
                ItemStack playerHead = makeHead(player);
                if (playerHead != null) {
//                    Main.getPlugin().getLogger().info("Player head generated at " + player.getLocation().toString() + " for " + player.getName());
                    currentWorld.dropItemNaturally(player.getLocation(), playerHead);
                }
            }
        }
    }
}