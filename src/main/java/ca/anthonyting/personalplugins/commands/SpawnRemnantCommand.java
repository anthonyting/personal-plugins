package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

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

        String username = args[0];

        Map<String, UUID> availablePlayers;
        try {
            availablePlayers = listPlayerDataFiles();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not list player data files for /spawnremnant.", e);
            sender.sendMessage(ChatColor.RED + "Player data could not be checked right now.");
            return true;
        }
        Map.Entry<String, UUID> match = availablePlayers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
        if (match == null) {
            sender.sendMessage(ChatColor.RED + "No player data found for " + username + ".");
            return true;
        }
        plugin.getRemnantManager().spawnForPlayer(sender, match.getValue(), match.getKey(), true);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        try {
            return listPlayerDataFiles().keySet().stream()
                    .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(prefix))
                    .toList();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not list player data files for /spawnremnant tab completion in "
                            + new File(plugin.getDataFolder(), "playerdata").getAbsolutePath(), e);
            return List.of();
        }
    }

    private Map<String, UUID> listPlayerDataFiles() throws IOException {
        File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.isDirectory()) {
            return Map.of();
        }

        Map<String, UUID> availablePlayers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Stream<java.nio.file.Path> files = java.nio.file.Files.list(playerDataFolder.toPath())) {
            files.filter(file -> file.getFileName().toString().endsWith(".yml"))
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        String fileId = fileName.substring(0, fileName.length() - 4);
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(fileId);
                        } catch (IllegalArgumentException ignored) {
                            plugin.getLogger().warning("Ignoring player data file with non-UUID filename: "
                                    + file.toAbsolutePath());
                            return;
                        }
                        String savedName = YamlConfiguration.loadConfiguration(file.toFile())
                                .getString("bukkit.lastKnownName");
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        String name = savedName != null ? savedName : offlinePlayer.getName();
                        if (name != null) {
                            UUID previous = availablePlayers.putIfAbsent(name, uuid);
                            if (previous != null && !previous.equals(uuid)) {
                                plugin.getLogger().warning("Ignoring duplicate player data name '" + name
                                        + "' in " + file.toAbsolutePath() + ".");
                            }
                        }
                    });
        }
        return availablePlayers;
    }
}