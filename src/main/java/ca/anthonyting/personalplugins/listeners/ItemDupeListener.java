package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ItemDupeListener implements Listener {

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent e) {
        if (e.getPlayer().isInsideVehicle() && (e.getPlayer().getVehicle() instanceof ChestedHorse) && ((ChestedHorse) e.getPlayer().getVehicle()).isCarryingChest()) {
            e.getPlayer().getVehicle().eject();
            Main.getPlugin().getLogger().warning(e.getPlayer().getName() + " disconnected while on a " + e.getPlayer().getVehicle().getType());
        }
    }
}
