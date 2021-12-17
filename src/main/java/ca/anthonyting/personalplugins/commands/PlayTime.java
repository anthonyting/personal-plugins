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

    private static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }

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

    private String getFormattedMessage(OfflinePlayer player, int position, boolean bold) {
        String boldString = "";
        if (bold) {
            boldString += ChatColor.DARK_AQUA.toString();
        }

        String prefix = boldString + position + ". " + player.getName() + ": ";
        return customPlayerHasPlayedMessage(prefix, player, " hours" + "\n" + ChatColor.RESET);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        boolean paginated = strings.length > 0 && isInteger(strings[0]);

        if (strings.length > 0 && !paginated) {
            Double timePlayed = getTimePlayedInHours(strings[0]);
            if (timePlayed == null) {
                commandSender.sendMessage(playerHasNotPlayedMessage(strings[0]));
            } else {
                commandSender.sendMessage(playerHasPlayedMessage(strings[0], timePlayed));
            }
            return true;
        }

        SortedMap<Double, OfflinePlayer> allPlayers = new TreeMap<>(Collections.reverseOrder());
        double totalTimePlayed = 0;
        double currentTimePlayed;
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            currentTimePlayed = getTimePlayedInHours(offlinePlayer);
            totalTimePlayed += currentTimePlayed;
            allPlayers.put(currentTimePlayed, offlinePlayer);
        }

        StringBuilder playersShown = new StringBuilder();

        boolean playerFound = false;
        Iterator<Map.Entry<Double, OfflinePlayer>> iterator = allPlayers.entrySet().iterator();
        Map.Entry<Double, OfflinePlayer> currentEntry;
        int lineCount = 0;
        int playerRank = 1;
        boolean bold;

        final int maxLineCount = 10;
        // line 1-9 summary
        // line 10 next page info
        final int regularPageMaxLines = maxLineCount - 1;
        // line 1: server total
        // line 2-8: summary
        // line 9: either summary or player depending on if player is in top 9
        // line 10: next page info
        final int firstPageMaxLines = maxLineCount - 2;
        int reservedMaxLines;
        int pageNumber = paginated ? Integer.parseInt(strings[0]) : 1;

        String serverTotal = ChatColor.GOLD + "Server total: " + ChatColor.RED + normalizeTimePlayed(totalTimePlayed) + " hours\n" + ChatColor.RESET;
        if (paginated && pageNumber > 1) {
            int iterateUntil = ((pageNumber - 2) * regularPageMaxLines) + firstPageMaxLines;

            if (iterateUntil > allPlayers.size() || iterateUntil < firstPageMaxLines) {
                displayMessage(commandSender, "There are no players on this page");
                return true;
            }

            while(iterator.hasNext() && playerRank < iterateUntil) {
                currentEntry = iterator.next();
                if (!playerFound && playerRank <= firstPageMaxLines && currentEntry.getValue().equals(commandSender)) {
                    // only check for player found if we are on the first page
                    playerFound = true;
                    // iterateUntil needs to take into account the player existing on the first page
                    iterateUntil++;
                }
                playerRank++;
            }

            reservedMaxLines = regularPageMaxLines;
        } else {
            reservedMaxLines = firstPageMaxLines;
            playersShown.append(serverTotal);
            lineCount++;
        }

        while (iterator.hasNext() && lineCount < reservedMaxLines) {
            currentEntry = iterator.next();
            if (!playerFound && currentEntry.getValue().equals(commandSender)) {
                bold = true;
                playerFound = true;
            } else {
                bold = false;
            }
            playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank++, bold));
            lineCount++;
        }

        if (!paginated && !playerFound && commandSender instanceof Player) {
            // always show this player
            while (iterator.hasNext() && lineCount <= maxLineCount - 1) {
                currentEntry = iterator.next();
                if (currentEntry.getValue().equals(commandSender)) {
                    // add new line if there is a next element and the line is not the last one
                    playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank, true));
                    lineCount++;
                    playerFound = true;
                }
                playerRank++;
            }
        } else {
            // show remaining lines if this player showed already
            while (iterator.hasNext() && lineCount < maxLineCount - 1) {
                currentEntry = iterator.next();
                playersShown.append(getFormattedMessage(currentEntry.getValue(), playerRank++, false));
                lineCount++;
            }
        }

        String nextPage = ChatColor.GOLD + "Type /playtime " + ChatColor.RED + (pageNumber > 0 ? pageNumber + 1 : 2) + ChatColor.GOLD + " to read the next page." + ChatColor.RESET;
        playersShown.append(nextPage);
        displayMessage(commandSender, playersShown.toString());

        return true;
    }

    private static void displayMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            for (String line : message.split("\\n")) {
                Bukkit.getLogger().info(line);
            }
        }
    }
}
