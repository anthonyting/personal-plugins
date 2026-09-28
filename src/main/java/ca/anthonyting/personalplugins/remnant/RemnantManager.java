package ca.anthonyting.personalplugins.remnant;

import ca.anthonyting.personalplugins.MainPlugin;
import ca.anthonyting.personalplugins.listeners.RemnantCompassListener;
import ca.anthonyting.personalplugins.listeners.RemnantDamageListener;
import ca.anthonyting.personalplugins.listeners.RemnantDeathListener;
import ca.anthonyting.personalplugins.listeners.RemnantJoinListener;
import ca.anthonyting.personalplugins.util.PlayerDataHandler;
import ca.anthonyting.personalplugins.util.PlayerDataHandler.RestoredPlayerData;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.World;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.io.IOException;

public class RemnantManager {

    private record SpawnPreparation(PlayerProfile profile, Chunk chunk) {
    }

    private final MainPlugin plugin;
    private final File configFile;
    private final YamlConfiguration config;
    private final NamespacedKey remnantKey;
    private final NamespacedKey compassKey;
    private final NamespacedKey compassOwnerKey;
    private final NamespacedKey compassInstanceKey;
    private final NamespacedKey remnantOwnerKey;
    private final NamespacedKey remnantTargetKey;
    private final NamespacedKey remnantInstanceKey;
    private final NamespacedKey dropKey;
    private final Set<UUID> pendingSpawns = new HashSet<>();

    public RemnantManager(MainPlugin plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "remnant.yml");
        if (!configFile.exists()) {
            try {
                if (!configFile.getParentFile().exists() && !configFile.getParentFile().mkdirs()) {
                    throw new IllegalStateException("Could not create plugin data folder: " + configFile.getParent());
                }
                if (!configFile.createNewFile()) {
                    throw new IllegalStateException("Could not create remnant config: " + configFile);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not create remnant config: " + configFile, e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        remnantKey = new NamespacedKey(plugin, "player_remnant");
        compassKey = new NamespacedKey(plugin, "remnant_compass");
        compassOwnerKey = new NamespacedKey(plugin, "remnant_compass_owner");
        compassInstanceKey = new NamespacedKey(plugin, "remnant_compass_instance");
        remnantOwnerKey = new NamespacedKey(plugin, "player_remnant_owner");
        remnantTargetKey = new NamespacedKey(plugin, "player_remnant_target");
        remnantInstanceKey = new NamespacedKey(plugin, "player_remnant_instance");
        dropKey = new NamespacedKey(plugin, "remnant_drop");
    }

    public void registerListeners() {
        var pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new RemnantDamageListener(this), plugin);
        pluginManager.registerEvents(new RemnantDeathListener(this), plugin);
        pluginManager.registerEvents(new RemnantCompassListener(plugin, this), plugin);
        pluginManager.registerEvents(new RemnantJoinListener(this), plugin);
    }

    public boolean reserveSpawn(UUID targetUuid) {
        if (findActiveRemnant(targetUuid) != null) {
            return false;
        }
        return pendingSpawns.add(targetUuid);
    }

    public void releaseSpawn(UUID targetUuid) {
        pendingSpawns.remove(targetUuid);
    }

    public void spawnForPlayer(Player recipient, UUID targetUuid, String username, boolean notifyMissingData) {
        if (!recipient.isOnline()) {
            return;
        }
        if (!notifyMissingData && hasSpawnedBefore(targetUuid)) {
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.getInventory().firstEmpty() < 0) {
            recipient.sendMessage(ChatColor.RED + (targetPlayer == recipient
                    ? "Make room in your inventory before summoning your remnant."
                    : username + " needs room in their inventory before the remnant can be summoned."));
            return;
        }
        if (!reserveSpawn(targetUuid)) {
            if (notifyMissingData) {
                recipient.sendMessage(ChatColor.RED + "A remnant of " + username + " is already here.");
            }
            return;
        }

        String profileName = Bukkit.getOfflinePlayer(targetUuid).getName();
        String canonicalUsername = profileName == null ? username : profileName;

        RestoredPlayerData data;
        try {
            data = PlayerDataHandler.loadFromDisk(plugin, targetUuid);
        } catch (IllegalStateException e) {
            releaseSpawn(targetUuid);
            if (e.getMessage() != null && e.getMessage().startsWith("No player data YAML found")) {
                if (notifyMissingData) {
                    recipient.sendMessage(ChatColor.RED + "No remnant of " + canonicalUsername + " can be found.");
                }
                plugin.getLogger().info("Cannot spawn remnant for " + canonicalUsername + ": no player data file exists.");
            } else {
                plugin.getLogger().severe("Failed to load player data for remnant " + canonicalUsername + ": " + e.getMessage());
                recipient.sendMessage(ChatColor.RED + "The remnant of " + canonicalUsername + " could not be reached.");
            }
            return;
        }

        Location location = data.location();
        if (notifyMissingData) {
            recipient.sendMessage(ChatColor.YELLOW + "Calling forth " + canonicalUsername + "'s remnant...");
        }

        try {
            PlayerProfile profile = Bukkit.getOfflinePlayer(targetUuid).getPlayerProfile();
            CompletableFuture<? extends PlayerProfile> profileLoad = profile.getTextures().getSkin() == null
                    ? profile.update()
                    : CompletableFuture.completedFuture(profile);

            profileLoad.thenCompose(loadedProfile ->
                            location.getWorld().getChunkAtAsync(location)
                                    .thenApply(chunk -> new SpawnPreparation(loadedProfile, chunk)))
                    .whenCompleteAsync((preparation, error) -> {
                        try {
                            if (error != null) {
                                plugin.getLogger().severe("Failed to prepare remnant for " + canonicalUsername + ": " + error.getMessage());
                                if (recipient.isOnline()) {
                                    recipient.sendMessage(ChatColor.RED + "The remnant of " + canonicalUsername + " could not be summoned.");
                                }
                                return;
                            }
                            if (!recipient.isOnline()) {
                                return;
                            }
                            Chunk chunk = preparation.chunk();
                            if (!chunk.isLoaded()) {
                                chunk = location.getWorld().getChunkAt(location);
                            }
                            boolean ticketAdded = chunk.addPluginChunkTicket(plugin);
                            try {
                                spawn(data, location, canonicalUsername, targetUuid, preparation.profile(), recipient);
                            } finally {
                                if (ticketAdded) {
                                    chunk.removePluginChunkTicket(plugin);
                                }
                            }
                        } catch (RuntimeException e) {
                            plugin.getLogger().severe("Failed to spawn remnant for " + canonicalUsername + ": " + e.getMessage());
                            if (recipient.isOnline()) {
                                recipient.sendMessage(ChatColor.RED + "The remnant of " + canonicalUsername + " could not be summoned.");
                            }
                        } finally {
                            releaseSpawn(targetUuid);
                        }
                    }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
        } catch (RuntimeException e) {
            releaseSpawn(targetUuid);
            plugin.getLogger().severe("Failed to prepare remnant for " + canonicalUsername + ": " + e.getMessage());
            recipient.sendMessage(ChatColor.RED + "The remnant of " + canonicalUsername + " could not be summoned.");
        }
    }

    public boolean spawn(RestoredPlayerData data, Location location, String username, UUID playerUuid,
                         PlayerProfile profile, Player player) {
        if (findActiveRemnant(playerUuid) != null) {
            plugin.getLogger().info("Denied duplicate remnant spawn for " + playerUuid + ".");
            player.sendMessage(ChatColor.RED + "A remnant of " + username + " is already here.");
            return false;
        }

        String remnantInstanceId = UUID.randomUUID().toString();
        Mannequin remnant = location.getWorld().spawn(location, Mannequin.class, entity -> {
            entity.setCustomName(username);
            entity.setCustomNameVisible(true);
            entity.setDescription(Component.text("Remnant", NamedTextColor.GRAY));
            entity.setProfile(ResolvableProfile.resolvableProfile((com.destroystokyo.paper.profile.PlayerProfile) profile));

            entity.setGravity(false);
            entity.setAI(false);
            entity.setImmovable(true);
            entity.setInvulnerable(false);
            entity.setPersistent(true);
            entity.setCanPickupItems(false);
            entity.setCollidable(false);
            entity.getPersistentDataContainer().set(remnantKey, PersistentDataType.BYTE, (byte) 1);
            entity.getPersistentDataContainer().set(remnantOwnerKey, PersistentDataType.STRING, playerUuid.toString());
            entity.getPersistentDataContainer().set(remnantTargetKey, PersistentDataType.STRING, playerUuid.toString());
            entity.getPersistentDataContainer().set(remnantInstanceKey, PersistentDataType.STRING, remnantInstanceId);

            AttributeInstance knockback = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(1.0);
            }

            AttributeInstance gravity = entity.getAttribute(Attribute.GRAVITY);
            if (gravity != null) {
                gravity.setBaseValue(0.0);
            }

            AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                entity.setHealth(maxHealth.getValue());
            }

            entity.setMetadata("player_config", new FixedMetadataValue(plugin, data.config()));
        });

        remnant.teleport(location);
        remnant.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        PlayerDataHandler.restoreEquipment(data.config(), remnant);

        saveActiveRemnant(playerUuid, location, username, remnantInstanceId);
        markSpawnedBefore(playerUuid);

        Player targetPlayer = Bukkit.getPlayer(playerUuid);
        if (targetPlayer != null) {
            giveCompassIfPossible(targetPlayer, remnant, targetPlayer != player);
        }

        String message = String.format(
                "Spawned remnant for %s (%s) at World: %s, X: %.2f, Y: %.2f, Z: %.2f (Yaw: %.1f, Pitch: %.1f)",
                username, playerUuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()
        );
        plugin.getLogger().info(message);
        if (targetPlayer == null) {
            plugin.getLogger().info("Remnant compass will be offered to " + username + " when they next join.");
        }
        player.sendMessage(ChatColor.GREEN + "A remnant of " + username + " has appeared."
                + (targetPlayer == player ? " Your compass points the way." : ""));
        return true;
    }

    public void ensureCompassForRemnant(Player player) {
        Map<String, Object> activeRemnant = findActiveRemnant(player.getUniqueId());
        if (activeRemnant == null) {
            return;
        }

        Object worldIdValue = activeRemnant.get("world");
        Object xValue = activeRemnant.get("x");
        Object yValue = activeRemnant.get("y");
        Object zValue = activeRemnant.get("z");
        Object usernameValue = activeRemnant.get("username");
        Object instanceValue = activeRemnant.get("instance");
        if (!(worldIdValue instanceof String worldId)
                || !(xValue instanceof Number x)
                || !(yValue instanceof Number y)
                || !(zValue instanceof Number z)
                || !(usernameValue instanceof String username)
                || !(instanceValue instanceof String instanceId)) {
            plugin.getLogger().warning("Cannot locate active remnant for " + player.getUniqueId() + ": invalid saved location.");
            return;
        }

        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Cannot locate active remnant for " + player.getUniqueId() + ": invalid world UUID.");
            return;
        }
        if (world == null) {
            plugin.getLogger().warning("Cannot locate active remnant for " + player.getUniqueId() + ": saved world is unavailable.");
            return;
        }

        Location location = new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
        giveCompassIfPossible(player, location, username, instanceId, true);
    }

    private void giveCompassIfPossible(Player player, Mannequin remnant, boolean announce) {
        String username = remnant.getCustomName();
        if (username == null) {
            username = Bukkit.getOfflinePlayer(player.getUniqueId()).getName();
        }
        if (username == null) {
            throw new IllegalStateException("Cannot create remnant compass: remnant and player names are unavailable.");
        }
        giveCompassIfPossible(
                player,
                remnant.getLocation(),
                username,
                remnant.getPersistentDataContainer().get(remnantInstanceKey, PersistentDataType.STRING),
                announce
        );
    }

    private void giveCompassIfPossible(Player player, Location location, String username, String instanceId, boolean announce) {
        if (instanceId == null || hasCompass(player, instanceId)) {
            return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(ChatColor.RED + "Make room in your inventory; your remnant compass is waiting.");
            return;
        }

        ItemStack compass = createCompass(location, username, player.getUniqueId(), instanceId);
        if (player.getInventory().addItem(compass).isEmpty()) {
            player.saveData();
            if (announce) {
                player.sendMessage(ChatColor.GREEN + "Your compass points to your remnant.");
            }
            plugin.getLogger().info("Issued non-droppable remnant compass to " + player.getName() + ".");
        }
    }

    private boolean hasCompass(Player player, String instanceId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCompassFor(item, instanceId)) {
                return true;
            }
        }
        return isCompassFor(player.getItemOnCursor(), instanceId);
    }

    private boolean isCompassFor(ItemStack item, String instanceId) {
        return item != null
                && item.hasItemMeta()
                && instanceId.equals(item.getItemMeta().getPersistentDataContainer().get(
                        compassInstanceKey, PersistentDataType.STRING));
    }

    private ItemStack createCompass(Location target, String username, UUID compassOwnerUuid, String remnantInstanceId) {
        ItemStack compass = new ItemStack(org.bukkit.Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + username + "'s Remnant");
        meta.setLore(List.of(ChatColor.GRAY + "Find your last self and reclaim what they carried."));
        meta.setLodestone(target);
        meta.setLodestoneTracked(false);
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(compassOwnerKey, PersistentDataType.STRING, compassOwnerUuid.toString());
        meta.getPersistentDataContainer().set(compassInstanceKey, PersistentDataType.STRING, remnantInstanceId);
        compass.setItemMeta(meta);
        return compass;
    }

    public boolean isRemnant(Mannequin remnant) {
        return remnant.getPersistentDataContainer().has(remnantKey, PersistentDataType.BYTE);
    }

    public void clearRemnant(UUID playerUuid) {
        markSpawnedBefore(playerUuid);
        List<Map<String, Object>> remnants = readActiveRemnants();
        if (remnants.removeIf(entry -> playerUuid.toString().equals(entry.get("uuid")))) {
            saveActiveRemnants(remnants);
        }
    }

    private void saveActiveRemnant(UUID playerUuid, Location location, String username, String instanceId) {
        List<Map<String, Object>> remnants = readActiveRemnants();
        remnants.removeIf(entry -> playerUuid.toString().equals(entry.get("uuid")));

        Map<String, Object> entry = new HashMap<>();
        entry.put("uuid", playerUuid.toString());
        entry.put("world", location.getWorld().getUID().toString());
        entry.put("x", location.getX());
        entry.put("y", location.getY());
        entry.put("z", location.getZ());
        entry.put("username", username);
        entry.put("instance", instanceId);
        remnants.add(entry);

        saveActiveRemnants(remnants);
    }

    private boolean hasSpawnedBefore(UUID playerUuid) {
        return config.getStringList("spawned-players").contains(playerUuid.toString());
    }

    private void markSpawnedBefore(UUID playerUuid) {
        List<String> spawnedPlayers = config.getStringList("spawned-players");
        if (!spawnedPlayers.contains(playerUuid.toString())) {
            spawnedPlayers.add(playerUuid.toString());
            config.set("spawned-players", spawnedPlayers);
            saveConfig();
        }
    }

    private Map<String, Object> findActiveRemnant(UUID playerUuid) {
        return readActiveRemnants().stream()
                .filter(entry -> playerUuid.toString().equals(entry.get("uuid")))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> readActiveRemnants() {
        Object stored = config.get("active-remnants");
        List<Map<String, Object>> result = new ArrayList<>();

        if (stored instanceof List<?> list) {
            Set<String> activeUuids = new HashSet<>();
            for (Object value : list) {
                if (value instanceof Map<?, ?> map) {
                    Map<String, Object> entry = new HashMap<>();
                    map.forEach((key, field) -> entry.put(String.valueOf(key), field));
                    if (entry.get("uuid") != null && activeUuids.add(String.valueOf(entry.get("uuid")))) {
                        result.add(entry);
                    }
                }
            }
            if (result.size() != list.size()) {
                plugin.getLogger().warning("Removed duplicate or invalid active remnant entries from remnant.yml.");
                saveActiveRemnants(result);
            }
            return result;
        }

        return result;
    }

    private void saveActiveRemnants(List<Map<String, Object>> remnants) {
        config.set("active-remnants", remnants);
        saveConfig();
    }

    public void queueCompassCleanup(String ownerId, String instanceId) {
        List<Map<?, ?>> pending = config.getMapList("pending-compass-cleanup");
        boolean alreadyQueued = pending.stream().anyMatch(entry ->
                ownerId.equals(String.valueOf(entry.get("owner")))
                        && instanceId.equals(String.valueOf(entry.get("instance")))
        );
        if (!alreadyQueued) {
            pending.add(Map.of("owner", ownerId, "instance", instanceId));
            config.set("pending-compass-cleanup", pending);
            saveConfig();
        }
    }

    public List<String> getPendingCompassCleanup(String ownerId) {
        return config.getMapList("pending-compass-cleanup").stream()
                .filter(entry -> ownerId.equals(String.valueOf(entry.get("owner"))))
                .map(entry -> String.valueOf(entry.get("instance")))
                .toList();
    }

    public void clearPendingCompassCleanup(String ownerId) {
        List<Map<String, String>> remaining = config.getMapList("pending-compass-cleanup").stream()
                .filter(entry -> !ownerId.equals(String.valueOf(entry.get("owner"))))
                .map(entry -> Map.of(
                        "owner", String.valueOf(entry.get("owner")),
                        "instance", String.valueOf(entry.get("instance"))
                ))
                .toList();
        config.set("pending-compass-cleanup", remaining);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save remnant config: " + configFile, e);
        }
    }

    public void clearRemnant(Mannequin remnant) {
        String targetUuid = remnant.getPersistentDataContainer().get(remnantTargetKey, PersistentDataType.STRING);
        if (targetUuid == null) {
            return;
        }
        try {
            clearRemnant(UUID.fromString(targetUuid));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Cannot clear remnant registry entry: invalid target UUID '" + targetUuid + "'.");
        }
    }

    public NamespacedKey getCompassKey() {
        return compassKey;
    }

    public NamespacedKey getCompassOwnerKey() {
        return compassOwnerKey;
    }

    public NamespacedKey getCompassInstanceKey() {
        return compassInstanceKey;
    }

    public NamespacedKey getRemnantOwnerKey() {
        return remnantOwnerKey;
    }

    public NamespacedKey getRemnantTargetKey() {
        return remnantTargetKey;
    }

    public NamespacedKey getRemnantInstanceKey() {
        return remnantInstanceKey;
    }

    public NamespacedKey getDropKey() {
        return dropKey;
    }

    public MainPlugin getPlugin() {
        return plugin;
    }
}
