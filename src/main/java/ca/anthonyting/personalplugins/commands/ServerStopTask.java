package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class ServerStopTask {

    private static final long MAX_STOP_SECONDS = TimeUnit.DAYS.toSeconds(3650);

    private final MainPlugin plugin;
    private BukkitTask stopTask;

    public ServerStopTask(MainPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean cancel() {
        if (stopTask == null || stopTask.isCancelled()) {
            return false;
        }
        stopTask.cancel();
        return true;
    }

    public void scheduleStop(long seconds) {
        if (seconds < 1 || seconds > MAX_STOP_SECONDS) {
            throw new IllegalArgumentException("Stop delay must be between 1 second and 3650 days.");
        }

        cancel();
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        long[] nextAnnouncementSecond = {nextAnnouncementSeconds(seconds)};
        broadcast(String.format("Restarting in %d seconds...", seconds));
        stopTask = new BukkitRunnable() {
            @Override
            public void run() {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    stopTask = null;
                    this.cancel();
                    broadcast("Server restarting...");
                    plugin.getServer().getOnlinePlayers().forEach(player ->
                            player.kickPlayer("Server restarting"));
                    plugin.getServer().shutdown();
                    return;
                }

                long secondsRemaining = remainingSeconds(remainingNanos);
                if (secondsRemaining <= nextAnnouncementSecond[0]) {
                    broadcast(String.format("%d...", secondsRemaining));
                    nextAnnouncementSecond[0] = nextAnnouncementSeconds(secondsRemaining);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        cancel();
    }

    private void broadcast(String message) {
        String prefix = "[" + ChatColor.GOLD + "Server" + ChatColor.RESET + "]";
        plugin.getServer().broadcastMessage(prefix + " " + message);
    }

    static long remainingSeconds(long remainingNanos) {
        return (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1);
    }

    static long nextAnnouncementSeconds(long secondsRemaining) {
        long intervalSeconds;
        long minimumAnnouncementSeconds;
        if (secondsRemaining > 3600) {
            intervalSeconds = 1800;
            minimumAnnouncementSeconds = 3600;
        } else if (secondsRemaining > 600) {
            intervalSeconds = 900;
            minimumAnnouncementSeconds = 600;
        } else if (secondsRemaining > 60) {
            intervalSeconds = 300;
            minimumAnnouncementSeconds = 60;
        } else if (secondsRemaining > 10) {
            intervalSeconds = 30;
            minimumAnnouncementSeconds = 10;
        } else {
            intervalSeconds = 1;
            minimumAnnouncementSeconds = 1;
        }

        long nextInterval = ((secondsRemaining - 1) / intervalSeconds) * intervalSeconds;
        return Math.max(minimumAnnouncementSeconds, nextInterval);
    }
}
