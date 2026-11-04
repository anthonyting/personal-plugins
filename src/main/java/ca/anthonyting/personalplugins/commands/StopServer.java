package ca.anthonyting.personalplugins.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StopServer implements CommandExecutor {

    private final ServerStopTask serverStopTask;

    public StopServer(ServerStopTask serverStopTask) {
        this.serverStopTask = serverStopTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 1) {
            return false;
        }

        var subcommand = strings[0];

        if (subcommand.equals("cancel")) {
            var cancelled = serverStopTask.cancel();
            if (cancelled) {
                commandSender.sendMessage("Stopping server was cancelled");
            } else {
                commandSender.sendMessage("No server stop task to cancel");
            }
            return true;
        }

        try {
            var parsedSeconds = Long.parseLong(subcommand);
            serverStopTask.scheduleStop(parsedSeconds);
            commandSender.sendMessage(String.format("Stopping server after %d seconds. Use `/stopserver cancel` to cancel", parsedSeconds));
        } catch (IllegalArgumentException e) {
            commandSender.sendMessage("Invalid argument. Accepted: [1-315360000 seconds] or [cancel]");
        }

        return true;
    }
}
