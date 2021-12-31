package ca.anthonyting.personalplugins.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EmojiMessage implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This must be run by a player");
            return false;
        }

        if (strings.length < 1) {
            commandSender.sendMessage("Must have at least one argument");
            return false;
        }

        StringBuilder message = new StringBuilder();
        for (var string : strings) {
            message.append(string).append(" ");
        }

        ((Player) commandSender).chat(message.toString());

        return true;
    }
}
