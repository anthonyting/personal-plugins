package ca.anthonyting.personalplugins.tabcomplete;

import ca.anthonyting.personalplugins.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetStatComplete implements TabCompleter {
    private static final Set<String> statisticNames = Arrays.stream(Statistic.values()).map(statistic -> statistic.name().toLowerCase()).collect(Collectors.toSet());

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(command.getName().equals("getstat")){
            if(commandSender instanceof Player player){
                if(player.hasPermission("personalplugins.getstat")) {
                    List<String> listAsString = new ArrayList<>();
                    if (strings.length == 1) {
                        for (String statisticName : statisticNames) {
                            if (statisticName.startsWith(strings[0])) {
                                listAsString.add(statisticName);
                            }
                        }
                    } else if (strings.length == 2) {
                        for (OfflinePlayer offlinePlayer : Players.getOfflinePlayersCached()) {
                            var name = offlinePlayer.getName();
                            if (strings[1].isBlank() || name != null && name.toLowerCase().startsWith(strings[1])) {
                                listAsString.add(offlinePlayer.getName());
                            }
                        }
                    }

                    return listAsString;
                }

            }
        }
        return null;
    }
}