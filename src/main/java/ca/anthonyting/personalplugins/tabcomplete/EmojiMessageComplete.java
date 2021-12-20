package ca.anthonyting.personalplugins.tabcomplete;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EmojiMessageComplete implements TabCompleter {

    private LinkedHashMap<String, Character> emojis;

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {

        if (emojis == null) {
            emojis = Main.getInstance().getEmojis();
        }

        if (commandSender instanceof Player && command.getName().equals("emoji") && commandSender.hasPermission("personalplugins.emoji")) {
            List<String> listAsString = new ArrayList<>();
            var currentArg = strings[strings.length - 1];
            for (var string : emojis.keySet()) {
                if (string.startsWith(currentArg)) {
                    listAsString.add(string);
                }
            }
            return listAsString;
        }

        return null;
    }
}
