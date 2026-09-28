package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.remnant.RemnantManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RemnantJoinListener implements Listener {

    private final RemnantManager remnants;

    public RemnantJoinListener(RemnantManager remnants) {
        this.remnants = remnants;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        remnants.ensureCompassForRemnant(player);
        remnants.spawnForPlayer(player, player.getUniqueId(), player.getName(), false);
    }
}
