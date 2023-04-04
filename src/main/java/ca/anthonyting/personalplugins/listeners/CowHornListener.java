package ca.anthonyting.personalplugins.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CowHornListener implements Listener {

    public static final String HORN_NAME = "Horn of the Storm";

    private final CowSacrificeListener cowSacrificeListener;

    public CowHornListener(CowSacrificeListener cowSacrificeListener) {
        this.cowSacrificeListener = cowSacrificeListener;
    }

    @EventHandler
    public void onHorn(PlayerInteractEvent event) {
        final var action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        final var item = event.getItem();
        if (item == null || item.getType() != Material.GOAT_HORN) {
            return;
        }

        final var itemMeta = item.getItemMeta();
        if (itemMeta == null || !itemMeta.getDisplayName().equalsIgnoreCase(HORN_NAME)) {
            return;
        }

        final var player = event.getPlayer();
        if (player.hasCooldown(Material.GOAT_HORN)) {
            return;
        }

        cowSacrificeListener.finalizeRitual(player);
    }
}
