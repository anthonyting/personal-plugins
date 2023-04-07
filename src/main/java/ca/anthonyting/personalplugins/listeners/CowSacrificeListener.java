package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.util.Region;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class ClearCowDeathCountTask extends BukkitRunnable {
    private long cowDeathCount = 0;
    private final Region region;
    private BossBar bossBar;
    private BukkitTask bossBarTickTask;
    private final long startedAt = System.currentTimeMillis();
    private Location lastCowLocation = null;
    public final Set<Player> playersThatStartedRitual = new HashSet<>();

    public ClearCowDeathCountTask(Region region) {
        this.region = region;
    }

    public long increment() {
        return ++cowDeathCount;
    }

    public Region getRegion() {
        return region;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public BukkitTask bossBarTickTask() {
        return bossBarTickTask;
    }

    public void setBossBarTickTask(BukkitTask bossBarTickTask) {
        this.bossBarTickTask = bossBarTickTask;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public Location getLastCowLocation() {
        return lastCowLocation;
    }

    public void setLastCowLocation(Location lastCowLocation) {
        this.lastCowLocation = lastCowLocation;
    }
}


public class CowSacrificeListener extends CancellableListener {
    private final Set<ClearCowDeathCountTask> cowDeathCountByRegion = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Player, ClearCowDeathCountTask> pendingRituals = new ConcurrentHashMap<>();
    private final MainPlugin plugin = MainPlugin.getInstance();
    private static final int RITUAL_COW_COUNT_REQUIRED = 150;
    private static final int RITUAL_COW_COUNT_BOSS_BAR_THRESHOLD = 60;
    private static final int RITUAL_COW_RADIUS_THRESHOLD = 20;
    private static final int RITUAL_CANCEL_DELAY_SECONDS = 60;

    public void cleanup() {
        cowDeathCountByRegion.forEach(ClearCowDeathCountTask::cancel);
        cowDeathCountByRegion.clear();
        pendingRituals.clear();
    }

    public void finalizeRitual(Player player) {
        final var task = pendingRituals.get(player);
        if (task == null) {
            player.sendMessage(ChatColor.RED + "You are not readys to complete the ritual.");
            return;
        }

        if (!task.getRegion().contains(player.getLocation())) {
            player.sendMessage(ChatColor.RED + "You have moved too far away from the ritual location.");
            return;
        }

        pendingRituals.remove(player);

        final var world = player.getWorld();

        task.cancel();

        final var playersThatStartedRitual = task.playersThatStartedRitual;
        // message that the ritual has been completed by all players that started the ritual
        final var message = new StringBuilder(ChatColor.GOLD + "A storm has been ");
        if (world.hasStorm()) {
            message.append("extended by ");
        } else {
            message.append("summoned by ");
        }

        if (playersThatStartedRitual.size() == 1) {
            message.append(player.getName());
        } else {
            var i = 0;
            for (var p : playersThatStartedRitual) {
                message.append(p.getName());
                // say and if it's the last player
                if (i == playersThatStartedRitual.size() - 2) {
                    message.append(", and ");
                } else if (i < playersThatStartedRitual.size() - 1) {
                    message.append(", ");
                }
                i++;
            }
        }

        world.strikeLightningEffect(task.getLastCowLocation());
        world.setStorm(true);
        world.setThundering(true);

        // show ritualistic particles at the last cow location
        world.spawnParticle(Particle.SPELL_WITCH, task.getLastCowLocation(), 100, 0.5, 0.5, 0.5, 0.5);

        // one minecraft day
        final var weatherDuration = 24000;
        world.setWeatherDuration(weatherDuration);
        world.setThunderDuration(weatherDuration);

        plugin.getServer().broadcastMessage(message.toString());
    }

    @EventHandler
    public void onCowDeath(EntityDeathEvent e) {
        final var entity = e.getEntity();
        if (entity.getType() != EntityType.COW) {
            return;
        }

        final var lastDamageCause = entity.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent)) {
            return;
        }

        final var damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
        if (damager.getType() != EntityType.PLAYER) {
            return;
        }

        final var player = (Player) damager;
        final var cow = (Cow) entity;
        final var world = cow.getWorld();

        if (!world.getEnvironment().equals(World.Environment.NORMAL)) {
            return;
        }

        final var location = cow.getLocation();
        final var task = cowDeathCountByRegion.stream()
                .filter(r -> r.getRegion().contains(location))
                .findFirst()
                .orElse(null);

        if (task == null || task.isCancelled()) {
            final var clearCowDeathCountTask = new ClearCowDeathCountTask(new Region(location, RITUAL_COW_RADIUS_THRESHOLD)) {
                public void cleanup() {
                    cowDeathCountByRegion.remove(this);
                    var bossBar = getBossBar();
                    if (bossBar != null) {
                        bossBar.removeAll();
                    }
                    final var region = getRegion();
                    final var bossBarKey = NamespacedKey.minecraft("ritual_progress_" + region.hashCode());
                    plugin.getServer().removeBossBar(bossBarKey);
                    final var bossBarRangeTask = bossBarTickTask();
                    if (bossBarRangeTask != null) {
                        bossBarRangeTask.cancel();
                    }
                    pendingRituals.remove(player);
                }

                @Override
                public void run() {
                    this.cleanup();
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    super.cancel();
                    this.cleanup();
                }
            };
            clearCowDeathCountTask.playersThatStartedRitual.add(player);
            cowDeathCountByRegion.add(clearCowDeathCountTask);
            clearCowDeathCountTask.runTaskLaterAsynchronously(plugin, RITUAL_CANCEL_DELAY_SECONDS * 20);
            return;
        }

        task.playersThatStartedRitual.add(player);
        final var count = task.increment();

        if (count == RITUAL_COW_COUNT_REQUIRED) {
            pendingRituals.put(player, task);
            task.setLastCowLocation(location);
            player.sendMessage(ChatColor.GOLD + "You have satisfied the ritual requirements. Blow the " + ChatColor.DARK_AQUA + ChatColor.ITALIC  + CowHornListener.HORN_NAME + ChatColor.RESET + ChatColor.GOLD + " to complete it.");
            return;
        }

        if (count > RITUAL_COW_COUNT_BOSS_BAR_THRESHOLD) {
            final var bossBarKey = NamespacedKey.minecraft("ritual_progress_" + task.getRegion().hashCode());
            // show progress as boss bar to damager
            var bossBar = plugin.getServer().getBossBar(bossBarKey);
            if (bossBar == null) {
                bossBar = plugin.getServer().createBossBar(
                        bossBarKey,
                        "Ritual progress",
                        BarColor.BLUE,
                        BarStyle.SOLID
                );
            }
            var progress = Math.max(Math.min((double) count / RITUAL_COW_COUNT_REQUIRED, 1), 0);
            bossBar.setProgress(progress);
            bossBar.addPlayer(player);
            task.setBossBar(bossBar);
            var bossBarTickTask = task.bossBarTickTask();
            if (bossBarTickTask != null) {
                return;
            }

            bossBarTickTask = new BukkitRunnable() {
                @Override
                public void run() {
                    final var bossBar = task.getBossBar();
                    if (bossBar == null) {
                        this.cancel();
                        return;
                    }
                    final var region = task.getRegion();
                    final var players = bossBar.getPlayers();
                    for (var player : players) {
                        if (!region.contains(player.getLocation())) {
                            bossBar.removePlayer(player);
                        }
                    }
                    // show remaining time
                    final var remainingTime = RITUAL_CANCEL_DELAY_SECONDS - (System.currentTimeMillis() - task.getStartedAt()) / 1000;
                    for (var player : task.playersThatStartedRitual) {
                        if (!players.contains(player) && region.contains(player.getLocation())) {
                            bossBar.addPlayer(player);
                        }
                    }
                    bossBar.setTitle("Ritual progress (" + remainingTime + "s)");
                }
            }.runTaskTimerAsynchronously(plugin, 0, 20);
            task.setBossBarTickTask(bossBarTickTask);
        }
    }
}
