package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class PlayTime implements CommandExecutor {

    private void messageTimePlayedInHours(CommandSender sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore() || target.getPlayer() == null) {
            sender.sendMessage(ChatColor.DARK_RED + target.getName() + " has never been on this server.");
            return;
        }
        int timePlayedInSeconds = target.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE)/20;
        int secondsInAnHour = 3600;
        String timePlayedInHours = String.format(Locale.ENGLISH, "%1$,.2f", ((double) timePlayedInSeconds) / secondsInAnHour);
        sender.sendMessage(ChatColor.RED + target.getName() + "Has played on this server for " + ChatColor.BLUE + timePlayedInHours + ChatColor.RED + " hours.");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        OfflinePlayer target = null;
        if (strings.length > 0) {
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (offlinePlayer.getName() != null && offlinePlayer.getName().equals(strings[0])) {
                    target = offlinePlayer;
                }
            }
        }

        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            messageTimePlayedInHours(p, p);
        } else if (target != null) {
            messageTimePlayedInHours(commandSender, target);
        }

        return true;
    }
}
