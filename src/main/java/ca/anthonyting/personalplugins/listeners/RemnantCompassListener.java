package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.remnant.RemnantManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class RemnantCompassListener implements Listener {

    private final MainPlugin plugin;
    private final RemnantManager remnants;

    public RemnantCompassListener(MainPlugin plugin, RemnantManager remnants) {
        this.plugin = plugin;
        this.remnants = remnants;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        boolean clickedPersonalInventory = clickedInventory == player.getInventory();
        boolean clickedExternalInventory = clickedInventory != null
                && !clickedPersonalInventory;
        boolean compassOnClickedSlot = isRemnantCompass(event.getCurrentItem());
        boolean compassOnCursor = isRemnantCompass(event.getCursor());
        boolean compassInHotbarSwap = event.getHotbarButton() >= 0
                && isRemnantCompass(player.getInventory().getItem(event.getHotbarButton()));
        boolean creativeCompassAction = event instanceof InventoryCreativeEvent
                && (compassOnClickedSlot || compassOnCursor);
        boolean shiftMovesCompassToContainer = clickedPersonalInventory
                && event.getView().getTopInventory().getType() != InventoryType.CRAFTING
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && compassOnClickedSlot;
        boolean dropsCompassFromCursor = clickedInventory == null
                && compassOnCursor
                && (event.getAction() == InventoryAction.DROP_ALL_CURSOR
                || event.getAction() == InventoryAction.DROP_ONE_CURSOR);
        boolean collectsCompassFromContainer = clickedExternalInventory
                && event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                && containsRemnantCompass(event.getView().getTopInventory());

        if ((clickedExternalInventory && (compassOnClickedSlot || compassOnCursor || compassInHotbarSwap))
                || shiftMovesCompassToContainer
                || dropsCompassFromCursor
                || collectsCompassFromContainer
                || creativeCompassAction) {
            deny(player, creativeCompassAction
                    ? "The compass is bound to you and cannot be copied."
                    : "The compass is bound to you and cannot be stored away.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isRemnantCompass(event.getOldCursor())) {
            return;
        }

        int topInventorySize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(rawSlot -> rawSlot < topInventorySize)) {
            deny(player, "The compass is bound to you and cannot be stored away.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isRemnantCompass(event.getItemDrop().getItemStack())) {
            deny(event.getPlayer(), "The compass is bound to you and cannot be dropped.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (isRemnantCompass(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
            plugin.getLogger().info("Deleted dropped remnant compass instead of allowing pickup by " + event.getPlayer().getName() + ".");
            event.getPlayer().sendMessage(ChatColor.RED + "The compass is bound to its bearer.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)
                && isRemnantCompass(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (isRemnantCompass(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isRemnantCompass(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        for (ItemStack drop : event.getDrops()) {
            if (isRemnantCompass(drop)) {
                event.getItemsToKeep().add(drop.clone());
                plugin.getLogger().info("Keeping remnant compass in " + event.getEntity().getName() + "'s inventory after respawn.");
            }
        }
        event.getDrops().removeIf(this::isRemnantCompass);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String ownerId = event.getPlayer().getUniqueId().toString();
        var instanceIds = remnants.getPendingCompassCleanup(ownerId);
        if (instanceIds.isEmpty()) {
            return;
        }

        int removed = 0;
        ItemStack[] contents = event.getPlayer().getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (instanceIds.stream().anyMatch(id -> isCompassFor(item, id))) {
                event.getPlayer().getInventory().setItem(slot, null);
                removed++;
            }
        }
        if (instanceIds.stream().anyMatch(id -> isCompassFor(event.getPlayer().getItemOnCursor(), id))) {
            event.getPlayer().setItemOnCursor(null);
            removed++;
        }

        remnants.clearPendingCompassCleanup(ownerId);
        if (removed > 0) {
            event.getPlayer().saveData();
        }
        plugin.getLogger().info("Removed " + removed + " expired remnant compass(es) for " + event.getPlayer().getName() + " on join.");
        event.getPlayer().sendMessage(ChatColor.GREEN + "Your remnant has fallen. Its compass fades away.");
    }

    private void deny(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        plugin.getLogger().info("Denied remnant compass action by " + player.getName() + ": " + message);
    }

    private boolean isRemnantCompass(ItemStack item) {
        return item != null
                && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(
                        remnants.getCompassKey(),
                        PersistentDataType.BYTE
                );
    }

    private boolean isCompassFor(ItemStack item, String instanceId) {
        if (!isRemnantCompass(item)) {
            return false;
        }
        return instanceId.equals(item.getItemMeta().getPersistentDataContainer().get(
                remnants.getCompassInstanceKey(),
                PersistentDataType.STRING
        ));
    }

    private boolean containsRemnantCompass(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (isRemnantCompass(item)) {
                return true;
            }
        }
        return false;
    }
}
