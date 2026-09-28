package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SpawnRemnantCommand implements CommandExecutor, TabCompleter {

    private final MainPlugin plugin;

    public SpawnRemnantCommand(MainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /spawnremnant <player>");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning("Denied /spawnremnant: command sender is not a player.");
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        String username = args[0];

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        UUID uuid = offlinePlayer.getUniqueId();
        String canonicalUsername = offlinePlayer.getName() == null ? username : offlinePlayer.getName();
        plugin.getRemnantManager().spawnForPlayer(player, uuid, canonicalUsername, true);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : Players.getOfflinePlayersCached()) {
            String name = offlinePlayer.getName();
            if (name != null && name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }
}