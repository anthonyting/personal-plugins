package ca.anthonyting.personalplugins.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PlayTime implements CommandExecutor {

    private String playerHasNotPlayedMessage(String playerName) {
        return ChatColor.DARK_AQUA + playerName + ChatColor.WHITE + " has never been on this server.";
    }

    private String playerHasPlayedMessage(String playerName, Double timePlayedInHours) {
        String timePlayedAsString = String.format(Locale.ENGLISH, "%1$,.2f", timePlayedInHours);
        return ChatColor.DARK_AQUA + playerName + ChatColor.WHITE + " has played on this server for " +
                ChatColor.DARK_AQUA + timePlayedAsString + ChatColor.WHITE + " hours.";
    }

    private Double getTimePlayedInHours(String targetName) {
        OfflinePlayer target = null;
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null &&
                    offlinePlayer.getName().toLowerCase(Locale.ENGLISH)
                            .equals(targetName.toLowerCase(Locale.ENGLISH))) {
                target = offlinePlayer;
                break;
            }
        }
        if (target == null) {
            return null;
        }
        int timePlayedInSeconds = target.getStatistic(Statistic.PLAY_ONE_MINUTE)/20;
        int secondsInAnHour = 3600;
        String timePlayedInHours = String.format(Locale.ENGLISH, "%1$,.2f", ((double) timePlayedInSeconds) / secondsInAnHour);
        return (double) timePlayedInSeconds / secondsInAnHour;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (strings.length > 0) {
            Double timePlayed = getTimePlayedInHours(strings[0]);
            if (timePlayed == null) {
                commandSender.sendMessage(playerHasNotPlayedMessage(strings[0]));
            } else {
                commandSender.sendMessage(playerHasPlayedMessage(strings[0], timePlayed));
            }
        } else if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            commandSender.sendMessage(playerHasPlayedMessage(p.getName(), getTimePlayedInHours(p.getName())));
        }

        return true;
    }
}
