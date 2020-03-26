package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.listeners.PlayerHeadListener;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;
import java.util.logging.Logger;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, Main.class, PlayerDeathEvent.class, EntityDamageByEntityEvent.class})
public class ValidExplosion
{
    @BeforeClass
    public static void setUp()
    {
        PowerMockito.mockStatic(Bukkit.class);

        ItemFactory itemFactory = Mockito.mock(ItemFactory.class);
        Mockito.when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        ItemMeta skullMeta = Mockito.mock(SkullMeta.class);
        Mockito.when(itemFactory.getItemMeta(Mockito.any())).thenReturn(skullMeta);
    }

    @Test
    public void testValidity()
    {

        PlayerDeathEvent playerDeath = PowerMockito.mock(PlayerDeathEvent.class);
        EntityDamageByEntityEvent playerDamagedByEntity = PowerMockito.mock(EntityDamageByEntityEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerUUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        Creeper creeper = Mockito.mock(Creeper.class);

        World world = Mockito.mock(World.class);

        Location playerLocation = new Location(world, 0, 64, 0);

        Mockito.when(world.getGameRuleValue(GameRule.DO_MOB_LOOT)).thenReturn(true);

        Mockito.when(creeper.isPowered()).thenReturn(true);
        Mockito.when(creeper.getWorld()).thenReturn(world);
        Mockito.when(creeper.getLocation()).thenReturn(playerLocation);

        Mockito.when(player.getWorld()).thenReturn(world);
        Mockito.when(player.getLocation()).thenReturn(playerLocation);
        Mockito.when(player.getUniqueId()).thenReturn(playerUUID);
        Mockito.when(player.getLastDamageCause()).thenReturn(playerDamagedByEntity);
        Mockito.when(playerDamagedByEntity.getDamager()).thenReturn(creeper);

        PlayerHeadListener playerHeadListener = new PlayerHeadListener();

        Logger log = Logger.getLogger("mockLogger");
        PowerMockito.mockStatic(Main.class);
        Main plugin = Mockito.mock(Main.class);

        Mockito.when(Main.getPlugin()).thenReturn(plugin);
        Mockito.when(plugin.getLogger()).thenReturn(log);

        playerHeadListener.onPlayerDeath(playerDeath);
    }
}
