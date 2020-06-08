package ca.anthonyting.personalplugins.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayTime implements CommandExecutor {

    private String playerHasNotPlayedMessage(String playerName) {
        return ChatColor.DARK_AQUA + playerName + ChatColor.WHITE + " has never been on this server.";
    }

    private String playerHasPlayedMessage(String playerName, Double timePlayedInHours) {
        String timePlayedAsString = String.format(Locale.ENGLISH, "%1$,.2f", timePlayedInHours);
        return ChatColor.DARK_AQUA + playerName + ChatColor.WHITE + " has played on this server for " +
                ChatColor.DARK_AQUA + timePlayedAsString + ChatColor.WHITE + " hours.";
    }

    private String customPlayerHasPlayedMessage(String prefix, OfflinePlayer target, String suffix) {
        return prefix + normalizeTimePlayed(getTimePlayedInHours(target)) + suffix;
    }

    private Double getTimePlayedInHours(OfflinePlayer offlinePlayer) {
        int timePlayedInSeconds = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE)/20;
        int secondsInAnHour = 3600;
        return (double) timePlayedInSeconds / secondsInAnHour;
    }

    private String normalizeTimePlayed(Double timePlayed) {
        return String.format(Locale.ENGLISH, "%1$,.2f", timePlayed);
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
        return (double) timePlayedInSeconds / secondsInAnHour;
    }

    private String getFormattedMessage(OfflinePlayer player, int position, boolean bold, boolean newline) {
        String boldString = "";
        if (bold) {
            boldString += ChatColor.DARK_AQUA.toString();
        }

        String prefix = boldString + position + ". " + player.getName() + ": ";
        return customPlayerHasPlayedMessage(prefix, player, " hours" + (newline ? "\n" : "") + ChatColor.RESET);
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
        } else {
            SortedMap<Double, OfflinePlayer> allPlayers = new TreeMap<>(Collections.reverseOrder());
            double totalTimePlayed = 0;
            double currentTimePlayed;
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                currentTimePlayed = getTimePlayedInHours(offlinePlayer);
                totalTimePlayed += currentTimePlayed;
                allPlayers.put(currentTimePlayed, offlinePlayer);
            }

            StringBuilder playersShown = new StringBuilder();
            String serverTotal = ChatColor.GOLD + "Server total: " + ChatColor.RED + normalizeTimePlayed(totalTimePlayed) + " hours\n" + ChatColor.RESET;

            final int maxLineCount = 10;
            boolean playerFound = false;
            Iterator<Map.Entry<Double, OfflinePlayer>> iterator = allPlayers.entrySet().iterator();
            Map.Entry<Double, OfflinePlayer> currentEntry;
            boolean bold;
            int lineCount = 1; // from serverTotal
            int playerRank = 1;
            while (iterator.hasNext() && (lineCount <= maxLineCount - 2)) { // reserve two lines for showing server total and self if not in top
                currentEntry = iterator.next();
                if (!playerFound && currentEntry.getValue().equals(commandSender)) {
                    bold = true;
                    playerFound = true;
                } else {
                    bold = false;
                }
                playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank++, bold, iterator.hasNext()));
                lineCount++;
            }

            if (!playerFound && commandSender instanceof Player) {
                // always show this player
                while (iterator.hasNext() && lineCount <= maxLineCount - 1) {
                    currentEntry = iterator.next();
                    if (currentEntry.getValue().equals(commandSender)) {
                        // add new line if there is a next element and the line is not the last one
                        playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank, true, iterator.hasNext() && lineCount != maxLineCount - 1));
                        lineCount++;
                        playerFound = true;
                    }
                    playerRank++;
                }
            } else {
                // show remaining lines if this player showed already
                while (iterator.hasNext() && lineCount <= maxLineCount - 1) {
                    currentEntry = iterator.next();
                    playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank++, false, iterator.hasNext() && lineCount != maxLineCount - 1));
                    lineCount++;
                }
            }
            if (commandSender instanceof Player) {
                commandSender.sendMessage(serverTotal + playersShown.toString());
            } else {
                for (String line : (serverTotal + playersShown.toString()).split("\\n")) {
                    Bukkit.getLogger().info(line);
                }
            }
        }

        return true;
    }
}
