package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public class ItemDupeListener implements Listener {

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Entity vehicle = p.getVehicle();
        if (p.isInsideVehicle() && (vehicle instanceof ChestedHorse) && ((ChestedHorse) vehicle).isCarryingChest()) {
            vehicle.eject();
            Main.getPlugin().getLogger().info(p.getName() + " disconnected while on a " + vehicle.getType().toString().toLowerCase(Locale.ENGLISH));
        }
    }
}
