package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creeper;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.function.Function;

public class PlayerHeadListener implements Listener {

    private Creeper previousCreeper = null;
    private final MainPlugin plugin = MainPlugin.getInstance();
    private final Function<Player, ItemStack> headFactory;

    public PlayerHeadListener() {
        this(PlayerHeadListener::makeHead);
    }

    PlayerHeadListener(Function<Player, ItemStack> headFactory) {
        this.headFactory = headFactory;
    }

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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // drop a player head if player killed by charged creeper
        Player player = event.getEntity();
        World currentWorld = player.getWorld();
        Boolean doMobLoot = currentWorld.getGameRuleValue(GameRules.MOB_DROPS);
        if (doMobLoot != null && doMobLoot) {
            dropHeadForChargedCreeper(player, currentWorld);
        }
    }

    void dropHeadForChargedCreeper(Player player, World currentWorld) {
        Creeper creeperThatKilledPlayer = chargedCreeperThatKilledEntity(player);
        if (creeperThatKilledPlayer != null) {
            if (creeperThatKilledPlayer == previousCreeper) {
                // only drop one head if multiple people are standing by
                return;
            }
            previousCreeper = creeperThatKilledPlayer;
            ItemStack playerHead = headFactory.apply(player);
            if (playerHead != null) {
                plugin.getLogger().info("Player head generated at " + player.getLocation() + " for " + player.getName());
                currentWorld.dropItemNaturally(player.getLocation(), playerHead);
            }
        }
    }
}