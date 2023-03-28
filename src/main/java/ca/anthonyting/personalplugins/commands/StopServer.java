package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class StopServer implements CommandExecutor {

    private final MainPlugin main = MainPlugin.getInstance();
    private BukkitTask stopTask = null;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 1) {
            return false;
        }

        var subcommand = strings[0];

        if (subcommand.equals("cancel")) {
            var cancelled = this.cancel();
            if (cancelled) {
                commandSender.sendMessage("Stopping server was cancelled");
            } else {
                commandSender.sendMessage("No server stop task to cancel");
            }
            return true;
        }

        try {
            var parsedSeconds = Long.parseLong(subcommand);
            commandSender.sendMessage(String.format("Stopping server after %d seconds. Use `/stopserver cancel` to cancel", parsedSeconds));
            this.stopServer(parsedSeconds);
        } catch (NumberFormatException e) {
            commandSender.sendMessage("Invalid argument. Accepted: [integer seconds] or [cancel]");
        }

        return true;
    }

    public boolean cancel() {
        if (stopTask != null) {
            stopTask.cancel();
            return true;
        }
        return false;
    }

    private void broadcast(final String message) {
        final var broadcastPrefix = "[" + ChatColor.GOLD + "Server" + ChatColor.RESET + "]";
        main.getServer().broadcastMessage(broadcastPrefix + " " + message);
    }

    private void stopServer(final long seconds) {
        if (stopTask != null && !stopTask.isCancelled()) {
            stopTask.cancel();
        }

        final long ONE_SECOND = 20;
        final long[] timeRemainingArray = {seconds};
        final var server = main.getServer();
        broadcast(String.format("Restarting in %d seconds...", seconds));
        stopTask = new BukkitRunnable() {
            @Override
            public void run() {
                timeRemainingArray[0]--;
                var timeRemaining = timeRemainingArray[0];
                if (timeRemaining <= 0) {
                    broadcast("Server restarting...");
                    this.cancel();
                    // Kicking player and shutting down must happen synchronously, otherwise there is an
                    // IllegalStateException on kick, since that cannot be asynchronous. Also, server shutdown should
                    // happen immediately after kick, otherwise players could theoretically rejoin.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            server.getOnlinePlayers().forEach(player -> {
                                player.kickPlayer("Server restarting");
                            });
                            server.shutdown();
                        }
                    }.runTask(main);
                } else {
                    broadcast(String.format("%d...", timeRemaining));
                }
            }
        }.runTaskTimerAsynchronously(main, ONE_SECOND, ONE_SECOND);
    }
}
