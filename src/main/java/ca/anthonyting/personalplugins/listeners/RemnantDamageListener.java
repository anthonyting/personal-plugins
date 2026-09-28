package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.remnant.RemnantManager;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public class RemnantDamageListener implements Listener {

    private final RemnantManager remnants;

    public RemnantDamageListener(RemnantManager remnants) {
        this.remnants = remnants;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRemnantDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Mannequin remnant) || !remnants.isRemnant(remnant)) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player attacker
                && isOwner(attacker, remnant)) {
            return;
        }

        event.setCancelled(true);
        remnant.setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRemnantInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Mannequin remnant && remnants.isRemnant(remnant)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRemnantInteractAt(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Mannequin remnant && remnants.isRemnant(remnant)) {
            event.setCancelled(true);
        }
    }

    private boolean isOwner(Player player, Mannequin remnant) {
        String targetId = remnant.getPersistentDataContainer().get(
                remnants.getRemnantTargetKey(),
                PersistentDataType.STRING
        );
        return player.getUniqueId().toString().equals(targetId);
    }
}
