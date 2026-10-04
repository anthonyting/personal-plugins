package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidExplosionTest {
    @Test
    void dropsPlayerHeadWhenKilledByChargedCreeper() {
        MainPlugin plugin = mock(MainPlugin.class);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        Player player = mock(Player.class);
        Creeper creeper = mock(Creeper.class);
        EntityDamageByEntityEvent damageEvent = mock(EntityDamageByEntityEvent.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);
        ItemStack head = mock(ItemStack.class);

        when(player.getLastDamageCause()).thenReturn(damageEvent);
        when(player.getLocation()).thenReturn(location);
        when(player.getName()).thenReturn("Player");
        when(damageEvent.getDamager()).thenReturn(creeper);
        when(creeper.isPowered()).thenReturn(true);

        try (MockedStatic<MainPlugin> mainPlugin = mockStatic(MainPlugin.class)) {
            mainPlugin.when(MainPlugin::getInstance).thenReturn(plugin);
            PlayerHeadListener listener = new PlayerHeadListener(ignored -> head);

            listener.dropHeadForChargedCreeper(player, world);
        }

        verify(world).dropItemNaturally(eq(location), eq(head));
    }
}
