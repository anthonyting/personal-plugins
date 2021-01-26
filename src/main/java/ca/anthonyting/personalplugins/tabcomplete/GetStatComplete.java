package ca.anthonyting.personalplugins.tabcomplete;

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

public class GetStatComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(command.getName().equals("getstat")){
            if(commandSender instanceof Player){
                final Player player = (Player) commandSender;
                if(player.hasPermission("personalplugins.getstat")) {
                    List<String> listAsString = new ArrayList<>();
                    if (strings.length == 1) {
                        Statistic[] list = Statistic.values();
                        for (Statistic stat : list) {
                            if (stat.name().toLowerCase().startsWith(strings[0])) {
                                listAsString.add(stat.name().toLowerCase());
                            }
                        }
                    } else if (strings.length == 2) {
                        OfflinePlayer[] list = Bukkit.getOfflinePlayers();
                        for (OfflinePlayer offlinePlayer : list) {
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