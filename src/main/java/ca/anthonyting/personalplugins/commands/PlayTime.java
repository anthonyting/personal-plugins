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

    private void messageTimePlayedInHours(CommandSender sender, String targetName) {
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
            sender.sendMessage(ChatColor.DARK_AQUA + targetName + ChatColor.WHITE + " has never been on this server.");
            return;
        }
        int timePlayedInSeconds = target.getStatistic(Statistic.PLAY_ONE_MINUTE)/20;
        int secondsInAnHour = 3600;
        String timePlayedInHours = String.format(Locale.ENGLISH, "%1$,.2f", ((double) timePlayedInSeconds) / secondsInAnHour);
        sender.sendMessage(ChatColor.DARK_AQUA + target.getName() + ChatColor.WHITE + " has played on this server for " + ChatColor.DARK_AQUA + timePlayedInHours + ChatColor.WHITE + " hours.");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (strings.length > 0) {
            messageTimePlayedInHours(commandSender, strings[0]);
        } else if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            messageTimePlayedInHours(p, p.getName());
        }

        return true;
    }
}
