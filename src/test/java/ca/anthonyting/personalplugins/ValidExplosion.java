package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.listeners.PlayerHeadListener;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.MockClassLoader;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.List;
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
        PowerMockito.mockStatic(Main.class, invocationOnMock -> null);

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


        Logger log = Logger.getLogger("mockLogger");

        JavaPlugin p = PowerMockito.mock(JavaPlugin.class);
        Mockito.when(p.getLogger()).thenReturn(log);
        Mockito.when(Main.getPlugin()).thenReturn(p);

        Mockito.when(playerDeath.getEntity()).thenReturn(player);

        PlayerHeadListener playerHeadListener = new PlayerHeadListener();
        playerHeadListener.onPlayerDeath(playerDeath);
    }
}
