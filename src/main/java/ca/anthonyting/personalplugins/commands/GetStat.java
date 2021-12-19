package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.exceptions.PlayerNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class GetStat implements CommandExecutor {

    private Integer getStatistic(OfflinePlayer offlinePlayer, String statisticName) {
        if (offlinePlayer == null || statisticName == null) {
            return null;
        }

        try {
            Statistic statistic = Statistic.valueOf(statisticName.toUpperCase());
            return offlinePlayer.getStatistic(statistic);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer getStatisticOfName(String targetName, String statisticName) throws PlayerNotFoundException {
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
            throw new PlayerNotFoundException("Player not found");
        }
        return getStatistic(target, statisticName);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (strings.length < 1) {
            return false;
        } else {
            if (commandSender instanceof Player) {
                final OfflinePlayer p = (OfflinePlayer) commandSender;
                Integer stat;
                if (strings.length == 1) {
                    stat = getStatistic(p, strings[0]);
                } else {
                    try {
                        stat = getStatisticOfName(strings[1], strings[0]);
                    } catch (PlayerNotFoundException e) {
                        commandSender.sendMessage(e.getMessage());
                        return true;
                    }
                }
                if (stat == null) {
                    commandSender.sendMessage("Stat does not exist.");
                } else {
                    String reply = strings[0] + ": " + stat;
                    if (strings.length > 1) {
                        commandSender.sendMessage(strings[1] + ": " + reply);
                    } else {
                        commandSender.sendMessage(reply);
                    }
                }
            }
        }

        return true;
    }
}
