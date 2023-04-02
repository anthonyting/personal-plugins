package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;

public class InspectionListener implements Listener {
    // TODO: don't use two maps, use a single map with a custom class
    private final ConcurrentHashMap<Player, Long> hitCountByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, BukkitTask> hitCountTasks = new ConcurrentHashMap<>();
    private final MainPlugin plugin = MainPlugin.getInstance();

    private String getPotionName(PotionMeta meta) {
        return meta.getBasePotionData().getType().toString().toLowerCase().replace("_", " ");
    }

    @EventHandler
    public void onInspection(EntityDamageByEntityEvent e) {
        var damager = e.getDamager();
        if (damager.getType() != EntityType.PLAYER) {
            plugin.getLogger().info("Damager is not a player");
            return;
        }

        var damagee = e.getEntity();
        if (damagee.getType() != EntityType.PLAYER) {
            plugin.getLogger().info("Damagee is not a player");
            return;
        }

        if (e.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK) {
            plugin.getLogger().info("Damage cause is not ENTITY_ATTACK");
            return;
        }

        var inspectedPlayer = (Player) damagee;
        var inspector = (Player) damager;

        // make sure inspector has permission
        if (!inspector.hasPermission("personalplugins.inspect")) {
            plugin.getLogger().info("Inspector does not have permission");
            return;
        }

        // make sure inspected player within inspector by 10 blocks, and they are both crouching
        var inspectedPlayerLocation = inspectedPlayer.getLocation();
        var inspectorLocation = inspector.getLocation();
        var distance = inspectedPlayerLocation.distance(inspectorLocation);
        if (distance > 10 || !inspectedPlayer.isSneaking() || !inspector.isSneaking()) {
            plugin.getLogger().info("Inspected player is not within 10 blocks of inspector");
            return;
        }

        // make sure damager is holding a stick
        var itemInHand = inspector.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.STICK) {
            plugin.getLogger().info("Inspector is not holding a stick");
            return;
        }

        // check if inspector is facing inspected player's back
        var inspectedPlayerEyeLocation = inspectedPlayer.getEyeLocation();
        var inspectorEyeLocation = inspector.getEyeLocation();
        var inspectedPlayerDirection = inspectedPlayerEyeLocation.getDirection();
        var inspectorDirection = inspectorEyeLocation.getDirection();
        var dotProduct = inspectedPlayerDirection.dot(inspectorDirection);
        if (dotProduct < 0) {
            plugin.getLogger().info("Inspector is not facing inspected player's back");
            return;
        }

        // require 3 hits to inspect
        var hitCount = hitCountByPlayer.getOrDefault(inspector, 0L);
        if (hitCount == 0) {
            // notify inspector and inspected player that inspection has started
            inspector.sendMessage(ChatColor.GREEN + "Started inspecting " + inspectedPlayer.getName() + "...");
            inspectedPlayer.sendMessage(ChatColor.GREEN + inspector.getName() + " is inspecting you...");
            var resetDelay = 20 * 10;
            var task = new BukkitRunnable() {
                @Override
                public void run() {

                    // if hit count is zero, then inspection has already been reset
                    if (hitCountByPlayer.getOrDefault(inspector, 0L) == 0) {
                        return;
                    }

                    // notify inspector that inspection has expired
                    inspector.sendMessage(ChatColor.RED + "Inspection of " + inspectedPlayer.getName() + " has expired.");
                    // notify inspected player that inspection is over
                    inspectedPlayer.sendMessage(ChatColor.RED + inspector.getName() + " is no longer inspecting you.");
                    // reset hit count
                    hitCountByPlayer.put(inspector, 0L);
                }
            }.runTaskLaterAsynchronously(plugin, resetDelay);
            hitCountTasks.put(inspector, task);
        } else if (hitCount == 1) {
            // notify inspector how many more hits are required
            inspector.sendMessage(ChatColor.GREEN + "One more round to inspect " + inspectedPlayer.getName() + "...");
        }
        hitCountByPlayer.put(inspector, ++hitCount);
        if (hitCount < 3) {
            return;
        }

        // reset hit count
        var task = hitCountTasks.get(inspector);
        if (task != null) {
            task.cancel();
        }
        hitCountByPlayer.put(inspector, 0L);

        // notify inspector and inspected player that they have been inspected
        var fullMessage = new StringBuilder()
                .append(ChatColor.GREEN)
                .append("Inspected ")
                .append(inspectedPlayer.getName())
                .append("!")
                .append("\n")
                .append(ChatColor.RED)
                .append("Suspicious items in ")
                .append(inspectedPlayer.getName())
                .append("'s inventory: ")
                .append(ChatColor.GOLD)
                .append("\n");

        var messageLength = fullMessage.length();

        var inventory = inspectedPlayer.getInventory();
        // TODO: clean up this code to use lists and loops

        // check suspicious items like potions
        inventory.all(Material.LINGERING_POTION).forEach((key, value) -> {
                    var meta = value.getItemMeta();
                    if (meta instanceof PotionMeta) {
                        var potionType = (PotionMeta) meta;
                        fullMessage
                                .append("- ")
                                .append(value.getAmount())
                                .append(" Lingering potion of ")
                                .append(getPotionName(potionType))
                                .append("\n");
                    }
                }
        );

        inventory.all(Material.SPLASH_POTION).forEach((key, value) -> {
                    var meta = value.getItemMeta();
                    if (meta instanceof PotionMeta) {
                        var potionType = (PotionMeta) meta;
                        fullMessage
                                .append("- ")
                                .append(value.getAmount())
                                .append(" Splash potion of ")
                                .append(getPotionName(potionType))
                                .append("\n");
                    }
                });
        // check suspicious blocks like TNT, end crystals, or TNT minecarts
        inventory.all(Material.TNT).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" TNT")
                        .append("\n")
        );
        inventory.all(Material.END_CRYSTAL).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" End crystal")
                        .append("\n")
        );
        inventory.all(Material.TNT_MINECART).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" TNT minecart")
                        .append("\n")
        );

        // check suspicious items like flint and steel, fire charges, or lava buckets
        inventory.all(Material.FLINT_AND_STEEL).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Flint and steel")
                        .append("\n")
        );
        inventory.all(Material.FIRE_CHARGE).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Fire charge")
                        .append("\n")
        );
        inventory.all(Material.LAVA_BUCKET).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Lava bucket")
                        .append("\n")
        );

        // check for pufferfish, suspicious soup
        inventory.all(Material.PUFFERFISH).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Pufferfish")
                        .append("\n")
        );
        inventory.all(Material.SUSPICIOUS_STEW).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Suspicious stew")
                        .append("\n")
        );

        // check for fireworks
        inventory.all(Material.FIREWORK_ROCKET).forEach((key, value) ->
                fullMessage
                        .append("- ")
                        .append(value.getAmount())
                        .append(" Firework rocket")
                        .append("\n")
        );

        boolean hasSuspiciousItems = fullMessage.length() > messageLength;
        if (!hasSuspiciousItems) {
            fullMessage.append("None");
        }

        // Note: suspicious item materials are:
        // - Material.LINGERING_POTION
        // - Material.SPLASH_POTION
        // - Material.TNT
        // - Material.END_CRYSTAL
        // - Material.TNT_MINECART
        // - Material.FLINT_AND_STEEL
        // - Material.FIRE_CHARGE
        // - Material.LAVA_BUCKET
        // - Material.PUFFERFISH
        // - Material.SUSPICIOUS_STEW
        // - Material.FIREWORK_ROCKET

        // notify inspector of suspicious items
        inspector.sendMessage(fullMessage.toString());
        // notify inspected player that the inspection has been completed
        inspectedPlayer.sendMessage(ChatColor.GREEN + inspector.getName() + " has inspected you!");
        // log the inspection
        plugin.getLogger().info(inspector.getName() + " inspected " + inspectedPlayer.getName() + " and found " + (hasSuspiciousItems ? "suspicious items" : "no suspicious items"));
    }
}
