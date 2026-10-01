package ca.anthonyting.personalplugins.util;

import ca.anthonyting.personalplugins.MainPlugin;
import com.google.gson.Gson;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.World;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.ShieldMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.map.MapCursor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataHandler {

    public static class PlayerDataNotFoundException extends IllegalStateException {
        public PlayerDataNotFoundException(String message) {
            super(message);
        }
    }

    private static final Gson GSON = new Gson();

    private record PlayerAttributeData(String id, Attribute attribute, double base,
                                       List<AttributeModifier> modifiers) {
    }

    private static final Set<String> KNOWN_ROOT_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround",
            "Dimension", "Invulnerable", "PortalCooldown", "UUID", "SelectedItemSlot",
            "Inventory", "EnderItems", "equipment", "abilities", "foodLevel", "foodSaturationLevel",
            "foodExhaustionLevel", "seenCredits", "playerGameType", "Score", "Brain", "RootVehicle",
            "XpLevel", "XpP", "XpTotal", "Bukkit", "bukkit", "foodTickTimer",
            "AbsorptionAmount", "Paper", "Spigot", "attributes", "WorldUUIDLeast", "WorldUUIDMost",
            "LastDeathLocation", "DeathTime", "recipeBook", "spawn_extra_particles_on_fall",
            "HurtByTimestamp", "respawn", "fall_distance", "XpSeed", "DataVersion",
            "SleepTimer", "FallFlying", "warden_spawn_tracker", "HurtTime", "Health",
            "current_impulse_context_reset_grace_time", "ignore_fall_damage_from_current_explosion",
            "Paper.OriginWorld", "Paper.Origin", "Paper.LastLogin", "Paper.LastSeen",
            "Paper.FireOverride", "Paper.SpawnReason", "Spigot.ticksLived"
    );

    private static final Set<String> KNOWN_ITEM_KEYS = Set.of(
            "id", "count", "Count", "slot", "Slot", "components"
    );

    private static final Set<String> KNOWN_COMPONENT_KEYS = Set.of(
            "minecraft:custom_name", "minecraft:lore", "minecraft:damage",
            "minecraft:enchantments", "minecraft:stored_enchantments",
            "minecraft:map_id", "minecraft:container", "minecraft:bundle_contents",
            "minecraft:repair_cost", "minecraft:trim", "minecraft:potion_contents",
            "minecraft:map_decorations", "minecraft:item_name", "minecraft:map_color",
            "minecraft:fireworks", "minecraft:bucket_entity_data",
            "minecraft:base_color", "minecraft:banner_patterns",
            "minecraft:instrument",
            "minecraft:tropical_fish/base_color", "minecraft:tropical_fish/pattern",
            "minecraft:tropical_fish/pattern_color", "minecraft:dyed_color"
    );

    /**
     * Record holding the loaded YamlConfiguration and the resolved spawn Location.
     */
    public record RestoredPlayerData(YamlConfiguration config, Location location) {}

    /**
     * Loads player data by username or UUID from the server's playerdata folder.
     */
    public static RestoredPlayerData loadFromDisk(MainPlugin plugin, String username, UUID uuid) throws IllegalStateException {
        YamlConfiguration config = loadConfigFromDisk(plugin, username, uuid);
        Location loc = parseLocation(config);
        return new RestoredPlayerData(config, loc);
    }

    public static CompletableFuture<YamlConfiguration> loadConfigFromDiskAsync(
            MainPlugin plugin, String username, UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadConfigFromDisk(plugin, username, uuid));
    }

    public static YamlConfiguration loadConfigFromDisk(MainPlugin plugin, String username, UUID uuid)
            throws IllegalStateException {
        File file = findPlayerDataFile(plugin, username, uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        logUnrecognizedRootKeys(config);
        return config;
    }

    private static File findPlayerDataFile(MainPlugin plugin, String username, UUID uuid) {
        File folder = new File(plugin.getDataFolder(), "playerdata");

        File fileByUsername = new File(folder, username + ".yml");
        File fileByUuid = new File(folder, uuid + ".yml");
        if (fileByUsername.exists()) {
            Bukkit.getLogger().info("[PlayerDataHandler] Loading player data from disk by username: " + fileByUsername.getAbsolutePath());
            return fileByUsername;
        }
        if (fileByUuid.exists()) {
            Bukkit.getLogger().info("[PlayerDataHandler] Loading player data from disk by UUID: " + fileByUuid.getAbsolutePath());
            return fileByUuid;
        }
        throw new PlayerDataNotFoundException(String.format(
                "No player data YAML found for '%s' (UUID: %s) in '%s'",
                username, uuid, folder.getAbsolutePath()
        ));
    }

    public static RestoredPlayerData loadFromDisk(MainPlugin plugin, UUID uuid) throws IllegalStateException {
        return loadFromDisk(plugin, uuid.toString(), uuid);
    }

    private static void logUnrecognizedRootKeys(YamlConfiguration config) {
        Set<String> rootKeys = config.getKeys(false);
        List<String> ignored = new ArrayList<>();
        for (String key : rootKeys) {
            if (!KNOWN_ROOT_KEYS.contains(key)) {
                ignored.add(key);
            }
        }
        if (!ignored.isEmpty()) {
            Bukkit.getLogger().warning("[PlayerDataHandler] Ignored/unrecognized root YAML keys found: " + ignored);
        } else {
            Bukkit.getLogger().info("[PlayerDataHandler] All root YAML keys recognized successfully.");
        }
    }

    public static Location parseLocation(YamlConfiguration config) throws IllegalStateException {
        if (!config.contains("Pos")) {
            throw new IllegalStateException("Missing mandatory 'Pos' coordinate list in player YAML data.");
        }

        List<Double> pos = config.getDoubleList("Pos");
        if (pos.size() < 3) {
            throw new IllegalStateException("Invalid 'Pos' array in player YAML data (expected 3 double elements).");
        }

        String dimension = config.getString("Dimension", "minecraft:overworld").replace("minecraft:", "");
        String worldName = dimension;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            World.Environment environment = switch (dimension) {
                case "overworld" -> World.Environment.NORMAL;
                case "the_nether", "nether" -> World.Environment.NETHER;
                case "the_end", "end" -> World.Environment.THE_END;
                default -> null;
            };
            if (environment != null) {
                world = Bukkit.getWorlds().stream()
                        .filter(candidate -> candidate.getEnvironment() == environment)
                        .findFirst()
                        .orElse(null);
            }
        }
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
            Bukkit.getLogger().warning("[PlayerDataHandler] Dimension world '" + dimension + "' not found. Falling back to default world: " + world.getName());
        }

        float yaw = 0.0f;
        float pitch = 0.0f;
        if (config.contains("Rotation")) {
            List<Float> rotation = config.getFloatList("Rotation");
            if (rotation.size() >= 2) {
                yaw = rotation.get(0);
                pitch = rotation.get(1);
            }
        }

        return new Location(world, pos.get(0), pos.get(1), pos.get(2), yaw, pitch);
    }

    public static List<ItemStack> getEnderChestItems(YamlConfiguration config) {
        List<ItemStack> items = new ArrayList<>();
        if (!config.contains("EnderItems")) {
            return items;
        }

        List<Map<?, ?>> rawEnderItems = config.getMapList("EnderItems");
        for (Map<?, ?> rawItem : rawEnderItems) {
            try {
                ItemStack item = parseRawItem(rawItem);
                if (item != null && !item.getType().isAir()) {
                    items.add(item);
                }
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Invalid Ender Chest item: " + e.getMessage(), e);
            }
        }
        return items;
    }

    public static List<ItemStack> getRemnantDrops(YamlConfiguration config) {
        List<ItemStack> items = new ArrayList<>();
        ConfigurationSection equipment = config.getConfigurationSection("equipment");

        for (Map<?, ?> rawItem : config.getMapList("Inventory")) {
            try {
                ItemStack item = parseRawItem(rawItem);
                if (item != null && !item.getType().isAir()) {
                    items.add(item);
                }
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Invalid remnant inventory item: " + e.getMessage(), e);
            }
        }

        if (equipment != null) {
            for (String slot : List.of("head", "chest", "legs", "feet", "offhand")) {
                if (!equipment.contains(slot)) {
                    continue;
                }
                Map<?, ?> itemMap = equipmentItemToMap(equipment.get(slot));
                if (itemMap == null) {
                    throw new IllegalStateException("Invalid remnant equipment data in slot '" + slot + "'.");
                }

                try {
                    ItemStack item = parseRawItem(itemMap);
                    if (item != null && !item.getType().isAir()) {
                        items.add(item);
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalStateException("Invalid remnant equipment in slot '" + slot + "': " + e.getMessage(), e);
                }
            }
        }

        items.addAll(getEnderChestItems(config));
        return items;
    }

    public static void validateRemnantData(YamlConfiguration config) {
        getRemnantDrops(config);
        parsePlayerAttributes(config);
    }

    public static int getRemnantExperienceDrop(YamlConfiguration config) {
        long totalExperience = Math.max(0, config.getLong("XpTotal", 0));
        return (int) Math.min(totalExperience, Integer.MAX_VALUE);
    }

    private static Map<?, ?> equipmentItemToMap(Object rawItem) {
        if (rawItem instanceof ConfigurationSection section) {
            return sectionToMap(section);
        }
        if (rawItem instanceof Map<?, ?> map) {
            return map;
        }
        return null;
    }

    public static ItemStack parseRawItem(Map<?, ?> rawItem) throws IllegalStateException {
        if (rawItem == null || rawItem.isEmpty()) {
            throw new IllegalStateException("Item map is null or empty.");
        }

        for (Object keyObj : rawItem.keySet()) {
            String key = String.valueOf(keyObj);
            if (!KNOWN_ITEM_KEYS.contains(key)) {
                throw new IllegalStateException("Unsupported item field '" + key + "' in item: " + rawItem.get("id"));
            }
        }

        Object idObj = rawItem.get("id");
        if (idObj == null) {
            throw new IllegalStateException("Missing 'id' identifier in item map: " + rawItem);
        }

        String idStr = String.valueOf(idObj).replace("minecraft:", "").toUpperCase();
        Material material = Material.matchMaterial(idStr);

        if (material == null) {
            throw new IllegalStateException("Invalid material type '" + idStr + "' found in item map.");
        }

        if (material.isAir() || !material.isItem()) {
            return null;
        }

        int amount = 1;
        Object countObj = rawItem.get("count");
        if (countObj == null) {
            countObj = rawItem.get("Count");
        }
        if (countObj instanceof Number num) {
            amount = num.intValue();
        } else if (countObj != null) {
            throw new IllegalStateException("Invalid item count: " + countObj);
        }

        if (amount <= 0) {
            throw new IllegalStateException("Item count must be greater than 0, found: " + amount);
        }

        ItemStack item = new ItemStack(material);
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        Object rawComponents = rawItem.get("components");
        if (rawComponents != null) {
            if (!(rawComponents instanceof Map<?, ?> componentsMap)) {
                throw new IllegalStateException("Invalid item components for '" + idStr + "'.");
            }
            if (meta == null) {
                throw new IllegalStateException("Item '" + idStr + "' does not support metadata components.");
            }
            parseComponents(componentsMap, meta);
        }
        if (meta != null) {
            item.setItemMeta(meta);
        }
        if (rawComponents instanceof Map<?, ?> componentsMap) {
            parseDirectItemComponents(componentsMap, item);
        }

        return item;
    }

    private static void parseDirectItemComponents(Map<?, ?> components, ItemStack item) {
        if (components.containsKey("minecraft:map_decorations")) {
            if (!(components.get("minecraft:map_decorations") instanceof Map<?, ?> rawDecorations)) {
                throw new IllegalStateException("Invalid map decorations component.");
            }
            MapDecorations.Builder decorations = MapDecorations.mapDecorations();
            for (Map.Entry<?, ?> entry : rawDecorations.entrySet()) {
                String id = String.valueOf(entry.getKey());
                if (!(entry.getValue() instanceof Map<?, ?> decoration)
                        || !(decoration.get("type") instanceof String typeId)
                        || !(decoration.get("x") instanceof Number x)
                        || !(decoration.get("z") instanceof Number z)
                        || !(decoration.get("rotation") instanceof Number rotation)
                        || !decoration.keySet().stream().map(String::valueOf)
                        .allMatch(Set.of("type", "x", "z", "rotation")::contains)) {
                    throw new IllegalStateException("Invalid map decoration entry '" + id + "'.");
                }
                NamespacedKey typeKey = NamespacedKey.fromString(typeId);
                MapCursor.Type type = typeKey == null ? null
                        : RegistryAccess.registryAccess().getRegistry(RegistryKey.MAP_DECORATION_TYPE).get(typeKey);
                if (type == null) {
                    throw new IllegalStateException("Unknown map decoration type '" + typeId + "'.");
                }
                decorations.put(id, MapDecorations.decorationEntry(
                        type, x.doubleValue(), z.doubleValue(), rotation.floatValue()));
            }
            item.setData(DataComponentTypes.MAP_DECORATIONS, decorations.build());
        }

        if (components.containsKey("minecraft:item_name")) {
            item.setData(DataComponentTypes.ITEM_NAME, parseItemName(components.get("minecraft:item_name")));
        }
    }

    private static Component parseItemName(Object rawName) {
        if (rawName instanceof String name) {
            return Component.text(name);
        }
        if (rawName instanceof Map<?, ?> nameMap) {
            return GsonComponentSerializer.gson().deserialize(GSON.toJson(nameMap));
        }
        throw new IllegalStateException("Unsupported or invalid item-name component: " + rawName);
    }

    private static void parseFireworks(Map<?, ?> fireworks, FireworkMeta meta) {
        for (Object keyObj : fireworks.keySet()) {
            String key = String.valueOf(keyObj);
            if (!Set.of("explosions", "flight_duration").contains(key)) {
                throw new IllegalStateException("Unsupported fireworks field '" + key + "'.");
            }
        }

        if (fireworks.containsKey("flight_duration")) {
            Object durationValue = fireworks.get("flight_duration");
            if (!(durationValue instanceof Number duration) || duration.intValue() < 0 || duration.intValue() > 127) {
                throw new IllegalStateException("Invalid firework flight duration: " + durationValue);
            }
            meta.setPower(duration.byteValue());
        }

        if (fireworks.containsKey("explosions")) {
            Object explosionsValue = fireworks.get("explosions");
            if (!(explosionsValue instanceof List<?> explosions)) {
                throw new IllegalStateException("Invalid firework explosions component.");
            }
            for (Object explosionValue : explosions) {
                if (!(explosionValue instanceof Map<?, ?> explosion)) {
                    throw new IllegalStateException("Invalid firework explosion entry: " + explosionValue);
                }
                meta.addEffect(parseFireworkExplosion(explosion));
            }
        }
    }

    private static FireworkEffect parseFireworkExplosion(Map<?, ?> explosion) {
        for (Object keyObj : explosion.keySet()) {
            String key = String.valueOf(keyObj);
            if (!Set.of("shape", "colors", "fade_colors", "has_trail", "has_twinkle").contains(key)) {
                throw new IllegalStateException("Unsupported firework explosion field '" + key + "'.");
            }
        }

        Object shapeValue = explosion.get("shape");
        if (!(shapeValue instanceof String shape)) {
            throw new IllegalStateException("Invalid firework explosion shape: " + shapeValue);
        }
        FireworkEffect.Type type;
        try {
            type = FireworkEffect.Type.valueOf(shape.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown firework explosion shape '" + shape + "'.", e);
        }

        FireworkEffect.Builder builder = FireworkEffect.builder().with(type);
        addFireworkColors(builder, explosion.get("colors"), "colors");
        addFireworkColors(builder, explosion.get("fade_colors"), "fade_colors");
        Object trailValue = explosion.get("has_trail");
        if (trailValue != null) {
            if (!(trailValue instanceof Boolean trail)) {
                throw new IllegalStateException("Invalid firework trail value: " + trailValue);
            }
            builder.trail(trail);
        }
        Object twinkleValue = explosion.get("has_twinkle");
        if (twinkleValue != null) {
            if (!(twinkleValue instanceof Boolean twinkle)) {
                throw new IllegalStateException("Invalid firework twinkle value: " + twinkleValue);
            }
            builder.flicker(twinkle);
        }
        return builder.build();
    }

    private static void addFireworkColors(FireworkEffect.Builder builder, Object rawColors, String field) {
        if (rawColors == null) {
            return;
        }
        if (!(rawColors instanceof List<?> colors)) {
            throw new IllegalStateException("Invalid firework " + field + " list.");
        }
        for (Object colorValue : colors) {
            if (!(colorValue instanceof Number color) || color.intValue() < 0 || color.intValue() > 0xFFFFFF) {
                throw new IllegalStateException("Invalid firework color: " + colorValue);
            }
            builder.withColor(Color.fromRGB(color.intValue()));
        }
    }

    public static void restoreEquipment(YamlConfiguration config, Mannequin remnant) {
        applyEquipmentToRemnant(config, remnant);
    }

    public static void restoreAttributes(YamlConfiguration config, Mannequin remnant) {
        List<String> unsupportedAttributes = new ArrayList<>();
        for (PlayerAttributeData attributeData : parsePlayerAttributes(config)) {
            AttributeInstance instance = remnant.getAttribute(attributeData.attribute());
            if (instance == null) {
                unsupportedAttributes.add(attributeData.id());
                continue;
            }
            instance.setBaseValue(attributeData.base());
            for (AttributeModifier modifier : attributeData.modifiers()) {
                instance.removeModifier(modifier.getKey());
                instance.addModifier(modifier);
            }
        }
        if (!unsupportedAttributes.isEmpty()) {
            Bukkit.getLogger().warning("[PlayerDataHandler] Player attributes not supported by mannequin and not applied: "
                    + unsupportedAttributes);
        }
    }

    private static List<PlayerAttributeData> parsePlayerAttributes(YamlConfiguration config) {
        Object rawAttributes = config.get("attributes");
        if (rawAttributes == null) {
            return List.of();
        }
        if (!(rawAttributes instanceof List<?> attributes)) {
            throw new IllegalStateException("Invalid player attributes: expected a list.");
        }

        List<PlayerAttributeData> parsedAttributes = new ArrayList<>();
        for (Object rawAttribute : attributes) {
            if (!(rawAttribute instanceof Map<?, ?> attributeMap)
                    || !(attributeMap.get("id") instanceof String id)
                    || !(attributeMap.get("base") instanceof Number base)
                    || !attributeMap.keySet().stream().map(String::valueOf)
                    .allMatch(Set.of("id", "base", "modifiers")::contains)
                    || !Double.isFinite(base.doubleValue())) {
                throw new IllegalStateException("Invalid player attribute entry: " + rawAttribute);
            }
            NamespacedKey key = NamespacedKey.fromString(id);
            Attribute attribute = key == null
                    ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(key);
            if (attribute == null) {
                throw new IllegalStateException("Unknown player attribute '" + id + "'.");
            }

            Object rawModifiers = attributeMap.get("modifiers");
            List<AttributeModifier> modifiers = new ArrayList<>();
            if (rawModifiers != null) {
                if (!(rawModifiers instanceof List<?> modifierList)) {
                    throw new IllegalStateException("Invalid modifiers for player attribute '" + id + "'.");
                }
                for (Object rawModifier : modifierList) {
                    if (!(rawModifier instanceof Map<?, ?> modifierMap)) {
                        throw new IllegalStateException("Invalid modifier for player attribute '" + id + "': " + rawModifier);
                    }
                    modifiers.add(parseAttributeModifier(modifierMap, id));
                }
            }
            parsedAttributes.add(new PlayerAttributeData(id, attribute, base.doubleValue(), List.copyOf(modifiers)));
        }
        return parsedAttributes;
    }

    private static AttributeModifier parseAttributeModifier(Map<?, ?> modifierMap, String attributeId) {
        if (!modifierMap.keySet().stream().map(String::valueOf)
                .allMatch(Set.of("id", "amount", "operation")::contains)) {
            throw new IllegalStateException("Unsupported modifier field for player attribute '" + attributeId + "'.");
        }
        Object idValue = modifierMap.get("id");
        Object amountValue = modifierMap.get("amount");
        Object operationValue = modifierMap.get("operation");
        if (!(idValue instanceof String id)
                || !(amountValue instanceof Number amount)
                || !(operationValue instanceof String operationName)
                || !Double.isFinite(amount.doubleValue())) {
            throw new IllegalStateException("Invalid modifier data for player attribute '" + attributeId + "'.");
        }
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) {
            throw new IllegalStateException("Invalid attribute modifier key '" + id + "'.");
        }
        AttributeModifier.Operation operation = switch (operationName) {
            case "add_value" -> AttributeModifier.Operation.ADD_NUMBER;
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_SCALAR;
            case "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> throw new IllegalStateException("Unsupported attribute modifier operation '"
                    + operationName + "'.");
        };
        return new AttributeModifier(key, amount.doubleValue(), operation);
    }

    private static Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                map.put(key, sectionToMap(child));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private static void applyEquipmentToRemnant(YamlConfiguration config, Mannequin remnant) {
        // 1. Armor/Elytra restoration from root 'equipment' map
        if (config.isConfigurationSection("equipment")) {
            ConfigurationSection equipmentSec = config.getConfigurationSection("equipment");
            if (equipmentSec != null) {
                for (String slot : List.of("head", "chest", "legs", "feet")) {
                    if (equipmentSec.contains(slot)) {
                        Map<?, ?> itemMap = null;

                        try {
                            if (equipmentSec.isConfigurationSection(slot)) {
                                itemMap = sectionToMap(equipmentSec.getConfigurationSection(slot));
                            } else if (!equipmentSec.getMapList(slot).isEmpty()) {
                                itemMap = equipmentSec.getMapList(slot).get(0);
                            } else {
                                throw new IllegalStateException("Equipment slot is not an item map.");
                            }

                            if (itemMap != null) {
                                ItemStack item = parseRawItem(itemMap);
                                if (item != null && !item.getType().isAir()) {
                                    switch (slot) {
                                        case "head" -> {
                                            remnant.getEquipment().setHelmet(item);
                                        }
                                        case "chest" -> {
                                            remnant.getEquipment().setChestplate(item);
                                        }
                                        case "legs" -> {
                                            remnant.getEquipment().setLeggings(item);
                                        }
                                        case "feet" -> {
                                            remnant.getEquipment().setBoots(item);
                                        }
                                    }

                                } else {
                                    Bukkit.getLogger().warning("[PlayerDataHandler] Equipment slot '" + slot + "' parsed to null or AIR.");
                                }
                            } else {
                                Bukkit.getLogger().warning("[PlayerDataHandler] Equipment slot '" + slot + "' could not be mapped to a valid item map.");
                            }
                        } catch (RuntimeException e) {
                            throw new IllegalStateException(
                                    "Could not restore remnant equipment slot '" + slot + "': " + e.getMessage(), e);
                        }
                    } else {
                    }
                }
            }
        } else {
            Bukkit.getLogger().warning("[PlayerDataHandler] No 'equipment' configuration section found in player data YAML!");
        }

        ConfigurationSection equipment = config.getConfigurationSection("equipment");
        if (equipment != null && equipment.contains("offhand")) {
            applyEquipmentItem(equipment.get("offhand"), "offhand", remnant);
        }

        // Scan Inventory for the selected main-hand item.
        if (config.contains("Inventory")) {
            int selectedSlot = config.contains("SelectedItemSlot") ? config.getInt("SelectedItemSlot") : 0;
            List<Map<?, ?>> rawInventory = config.getMapList("Inventory");

            for (Map<?, ?> rawItem : rawInventory) {
                if (!(rawItem.get("Slot") instanceof Number slotValue)) {
                    throw new IllegalStateException("Remnant inventory entry has no numeric Slot field.");
                }

                int slot = slotValue.intValue();

                try {
                    ItemStack item = parseRawItem(rawItem);
                    if (item == null || item.getType().isAir()) continue;

                    if (slot == selectedSlot) {
                        remnant.getEquipment().setItemInMainHand(item);
                    }

                } catch (RuntimeException e) {
                    throw new IllegalStateException(
                            "Could not restore remnant inventory slot " + slot + ": " + e.getMessage(), e);
                }
            }
        } else {
        }
    }

    private static void applyEquipmentItem(Object rawItem, String slot, Mannequin remnant) {
        Map<?, ?> itemMap = equipmentItemToMap(rawItem);
        if (itemMap == null) {
            Bukkit.getLogger().warning("[PlayerDataHandler] Equipment slot '" + slot + "' could not be mapped to a valid item map.");
            return;
        }

        try {
            ItemStack item = parseRawItem(itemMap);
            if (item == null || item.getType().isAir()) {
                return;
            }
            remnant.getEquipment().setItemInOffHand(item);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Invalid remnant equipment slot '" + slot + "': " + e.getMessage(), e);
        }
    }

    private static void parseComponents(Map<?, ?> components, ItemMeta meta) {
        for (Object keyObj : components.keySet()) {
            String key = String.valueOf(keyObj);
            if (!KNOWN_COMPONENT_KEYS.contains(key)) {
                throw new IllegalStateException("Unsupported item component '" + key + "'.");
            }
        }

        if (components.containsKey("minecraft:custom_name")) {
            meta.displayName(parseItemName(components.get("minecraft:custom_name")));
        }

        if (components.containsKey("minecraft:fireworks")) {
            if (!(components.get("minecraft:fireworks") instanceof Map<?, ?> fireworks)
                    || !(meta instanceof FireworkMeta fireworkMeta)) {
                throw new IllegalStateException("Unsupported or invalid fireworks component.");
            }
            parseFireworks(fireworks, fireworkMeta);
        }

        if (components.containsKey("minecraft:bucket_entity_data")) {
            parseBucketEntityData(components.get("minecraft:bucket_entity_data"));
        }

        if (components.containsKey("minecraft:tropical_fish/base_color")
                || components.containsKey("minecraft:tropical_fish/pattern")
                || components.containsKey("minecraft:tropical_fish/pattern_color")) {
            if (!(meta instanceof TropicalFishBucketMeta fishMeta)) {
                throw new IllegalStateException("Tropical-fish components are only supported on tropical fish buckets.");
            }
            if (components.containsKey("minecraft:tropical_fish/base_color")) {
                fishMeta.setBodyColor(parseDyeColor(
                        components.get("minecraft:tropical_fish/base_color"), "base_color"));
            }
            if (components.containsKey("minecraft:tropical_fish/pattern_color")) {
                fishMeta.setPatternColor(parseDyeColor(
                        components.get("minecraft:tropical_fish/pattern_color"), "pattern_color"));
            }
            if (components.containsKey("minecraft:tropical_fish/pattern")) {
                Object patternValue = components.get("minecraft:tropical_fish/pattern");
                if (!(patternValue instanceof String patternName)) {
                    throw new IllegalStateException("Invalid tropical-fish pattern: " + patternValue);
                }
                try {
                    fishMeta.setPattern(TropicalFish.Pattern.valueOf(patternName.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Unknown tropical-fish pattern '" + patternName + "'.", e);
                }
            }
        }

        if (components.containsKey("minecraft:lore")) {
            if (!(components.get("minecraft:lore") instanceof List<?> loreList)) {
                throw new IllegalStateException("Invalid item lore component.");
            }
            List<String> lore = loreList.stream().map(Object::toString).toList();
            meta.setLore(lore);
        }

        if (components.containsKey("minecraft:damage")) {
            if (!(components.get("minecraft:damage") instanceof Number damageValue) || !(meta instanceof Damageable damageable)) {
                throw new IllegalStateException("Unsupported or invalid item damage component.");
            }
            int damage = damageValue.intValue();
            damageable.setDamage(damage);
        }

        if (components.containsKey("minecraft:dyed_color")) {
            Object colorValue = components.get("minecraft:dyed_color");
            if (!(colorValue instanceof Number color) || !(meta instanceof LeatherArmorMeta leatherArmorMeta)
                    || color.intValue() < 0 || color.intValue() > 0xFFFFFF) {
                throw new IllegalStateException("Unsupported or invalid dyed-color component: " + colorValue);
            }
            leatherArmorMeta.setColor(Color.fromRGB(color.intValue()));
        }

        if (components.containsKey("minecraft:base_color")) {
            Object colorValue = components.get("minecraft:base_color");
            if (!(meta instanceof ShieldMeta shieldMeta)) {
                throw new IllegalStateException("Base-color component is only supported on shields.");
            }
            shieldMeta.setBaseColor(parseDyeColor(colorValue, "base_color"));
        }

        if (components.containsKey("minecraft:banner_patterns")) {
            if (!(components.get("minecraft:banner_patterns") instanceof List<?> rawPatterns)
                    || !(meta instanceof BannerMeta bannerMeta)) {
                throw new IllegalStateException("Unsupported or invalid banner-patterns component.");
            }
            bannerMeta.setPatterns(parseBannerPatterns(rawPatterns));
        }

        if (components.containsKey("minecraft:instrument")) {
            Object instrumentValue = components.get("minecraft:instrument");
            if (!(instrumentValue instanceof String instrumentId)
                    || !(meta instanceof MusicInstrumentMeta instrumentMeta)) {
                throw new IllegalStateException("Unsupported or invalid instrument component: " + instrumentValue);
            }
            NamespacedKey instrumentKey = NamespacedKey.fromString(instrumentId);
            org.bukkit.MusicInstrument instrument = instrumentKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT).get(instrumentKey);
            if (instrument == null) {
                throw new IllegalStateException("Unknown music instrument '" + instrumentId + "'.");
            }
            instrumentMeta.setInstrument(instrument);
        }

        if (components.containsKey("minecraft:repair_cost")) {
            if (!(components.get("minecraft:repair_cost") instanceof Number repairCostValue) || !(meta instanceof Repairable repairable)) {
                throw new IllegalStateException("Unsupported or invalid item repair-cost component.");
            }
            int repairCost = repairCostValue.intValue();
            repairable.setRepairCost(repairCost);
        }

        if (components.containsKey("minecraft:trim")) {
            if (!(components.get("minecraft:trim") instanceof Map<?, ?> trimMap)
                    || !(meta instanceof ArmorMeta armorMeta)) {
                throw new IllegalStateException("Unsupported or invalid armor-trim component.");
            }
            Object materialValue = trimMap.get("material");
            Object patternValue = trimMap.get("pattern");
            if (!(materialValue instanceof String materialId) || !(patternValue instanceof String patternId)) {
                throw new IllegalStateException("Armor-trim component must specify material and pattern keys.");
            }

            NamespacedKey materialKey = NamespacedKey.fromString(materialId);
            NamespacedKey patternKey = NamespacedKey.fromString(patternId);
            TrimMaterial trimMaterial = materialKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(materialKey);
            TrimPattern trimPattern = patternKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(patternKey);
            if (trimMaterial == null || trimPattern == null) {
                throw new IllegalStateException("Unknown armor trim material or pattern: "
                        + materialId + " / " + patternId);
            }
            armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
        }

        if (components.containsKey("minecraft:enchantments")) {
            if (!(components.get("minecraft:enchantments") instanceof Map<?, ?> enchants)) {
                throw new IllegalStateException("Invalid item enchantments component.");
            }
            parseEnchantmentMap(enchants, meta, false);
        }

        if (components.containsKey("minecraft:stored_enchantments")) {
            if (!(components.get("minecraft:stored_enchantments") instanceof Map<?, ?> storedEnchants)) {
                throw new IllegalStateException("Invalid stored-enchantments component.");
            }
            parseEnchantmentMap(storedEnchants, meta, true);
        }

        if (components.containsKey("minecraft:map_id")) {
            if (!(components.get("minecraft:map_id") instanceof Number mapIdValue) || !(meta instanceof MapMeta mapMeta)) {
                throw new IllegalStateException("Unsupported or invalid map-id component.");
            }
            int mapId = mapIdValue.intValue();
            mapMeta.setMapId(mapId);
        }

        if (components.containsKey("minecraft:map_color")) {
            if (!(components.get("minecraft:map_color") instanceof Number colorValue)
                    || !(meta instanceof MapMeta mapMeta)) {
                throw new IllegalStateException("Unsupported or invalid map-color component.");
            }
            mapMeta.setColor(Color.fromRGB(colorValue.intValue()));
        }

        if (components.containsKey("minecraft:container")) {
            if (!(components.get("minecraft:container") instanceof List<?> containerList)
                    || !(meta instanceof BlockStateMeta blockStateMeta)) {
                throw new IllegalStateException("Unsupported or invalid item container component.");
            }
            parseContainerContents(containerList, blockStateMeta);
        }

        if (components.containsKey("minecraft:bundle_contents")) {
            if (!(components.get("minecraft:bundle_contents") instanceof List<?> bundleContents)
                    || !(meta instanceof BundleMeta bundleMeta)) {
                throw new IllegalStateException("Unsupported or invalid bundle contents component.");
            }
            parseBundleContents(bundleContents, bundleMeta);
        }

        if (components.containsKey("minecraft:potion_contents")) {
            if (!(components.get("minecraft:potion_contents") instanceof Map<?, ?> potionContents)
                    || !(meta instanceof PotionMeta potionMeta)) {
                throw new IllegalStateException("Unsupported or invalid potion contents component.");
            }
            parsePotionContents(potionContents, potionMeta);
        }
    }

    private static void parseBucketEntityData(Object rawEntityData) {
        if (!(rawEntityData instanceof Map<?, ?> entityData)) {
            throw new IllegalStateException("Invalid bucket entity data: expected a map.");
        }
        for (Object keyObj : entityData.keySet()) {
            String key = String.valueOf(keyObj);
            if (!Set.of("Health").contains(key)) {
                throw new IllegalStateException("Unsupported bucket entity data field '" + key + "'.");
            }
        }
        if (entityData.containsKey("Health")) {
            Object rawHealth = entityData.get("Health");
            if (!(rawHealth instanceof Number health) || !Double.isFinite(health.doubleValue())
                    || health.doubleValue() < 0.0) {
                throw new IllegalStateException("Invalid bucket entity health: " + rawHealth);
            }
            if (health.doubleValue() != 3.0) {
                throw new IllegalStateException("Cannot preserve non-default bucket entity health: " + rawHealth);
            }
        }
    }

    private static org.bukkit.DyeColor parseDyeColor(Object rawColor, String field) {
        if (!(rawColor instanceof String colorName)) {
            throw new IllegalStateException("Invalid tropical-fish " + field + ": " + rawColor);
        }
        try {
            return org.bukkit.DyeColor.valueOf(colorName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown tropical-fish " + field + " '" + colorName + "'.", e);
        }
    }

    private static List<Pattern> parseBannerPatterns(List<?> rawPatterns) {
        List<Pattern> patterns = new ArrayList<>();
        for (Object rawPattern : rawPatterns) {
            if (!(rawPattern instanceof Map<?, ?> patternMap)
                    || patternMap.size() != 2
                    || !(patternMap.get("color") instanceof String colorName)
                    || !(patternMap.get("pattern") instanceof String patternId)) {
                throw new IllegalStateException("Invalid banner pattern entry: " + rawPattern);
            }
            org.bukkit.DyeColor color = parseDyeColor(colorName, "banner pattern color");
            NamespacedKey patternKey = NamespacedKey.fromString(patternId);
            org.bukkit.block.banner.PatternType pattern = patternKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).get(patternKey);
            if (pattern == null) {
                throw new IllegalStateException("Unknown banner pattern '" + patternId + "'.");
            }
            patterns.add(new Pattern(color, pattern));
        }
        return patterns;
    }

    private static void parsePotionContents(Map<?, ?> potionContents, PotionMeta potionMeta) {
        for (Object keyObj : potionContents.keySet()) {
            String key = String.valueOf(keyObj);
            if (!Set.of("potion", "custom_color", "custom_effects").contains(key)) {
                throw new IllegalStateException("Unsupported potion contents field '" + key + "'.");
            }
        }

        if (potionContents.containsKey("potion")) {
            Object potionValue = potionContents.get("potion");
            if (!(potionValue instanceof String potionId)) {
                throw new IllegalStateException("Invalid base potion identifier: " + potionValue);
            }
            NamespacedKey potionKey = NamespacedKey.fromString(potionId);
            PotionType potionType = potionKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION).get(potionKey);
            if (potionType == null) {
                throw new IllegalStateException("Unknown base potion '" + potionId + "'.");
            }
            potionMeta.setBasePotionType(potionType);
        }

        if (potionContents.containsKey("custom_color")) {
            Object colorValue = potionContents.get("custom_color");
            if (!(colorValue instanceof Number color)) {
                throw new IllegalStateException("Invalid custom potion color: " + colorValue);
            }
            potionMeta.setColor(Color.fromRGB(color.intValue()));
        }

        if (potionContents.containsKey("custom_effects")) {
            if (!(potionContents.get("custom_effects") instanceof List<?> effects)) {
                throw new IllegalStateException("Invalid custom potion effects.");
            }
            for (Object rawEffect : effects) {
                if (!(rawEffect instanceof Map<?, ?> effectMap)) {
                    throw new IllegalStateException("Invalid custom potion effect: " + rawEffect);
                }
                potionMeta.addCustomEffect(parsePotionEffect(effectMap), true);
            }
        }
    }

    private static PotionEffect parsePotionEffect(Map<?, ?> effectMap) {
        for (Object keyObj : effectMap.keySet()) {
            String key = String.valueOf(keyObj);
            if (!Set.of("id", "amplifier", "duration", "ambient", "show_particles", "show_icon").contains(key)) {
                throw new IllegalStateException("Unsupported custom potion effect field '" + key + "'.");
            }
        }

        Object idValue = effectMap.get("id");
        Object amplifierValue = effectMap.get("amplifier");
        Object durationValue = effectMap.get("duration");
        if (!(idValue instanceof String id)
                || !(amplifierValue instanceof Number amplifier)
                || !(durationValue instanceof Number duration)) {
            throw new IllegalStateException("Custom potion effect must specify id, amplifier, and duration.");
        }
        NamespacedKey effectKey = NamespacedKey.fromString(id);
        PotionEffectType effectType = effectKey == null ? null : PotionEffectType.getByKey(effectKey);
        if (effectType == null) {
            throw new IllegalStateException("Unknown custom potion effect '" + id + "'.");
        }
        return new PotionEffect(
                effectType,
                duration.intValue(),
                amplifier.intValue(),
                getBoolean(effectMap, "ambient", false),
                getBoolean(effectMap, "show_particles", true),
                getBoolean(effectMap, "show_icon", true)
        );
    }

    private static boolean getBoolean(Map<?, ?> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalStateException("Expected boolean value for '" + key + "', found: " + value);
        }
        return booleanValue;
    }

    private static void parseBundleContents(List<?> bundleContents, BundleMeta bundleMeta) {
        List<ItemStack> items = new ArrayList<>(bundleContents.size());
        for (int index = 0; index < bundleContents.size(); index++) {
            Object rawItem = bundleContents.get(index);
            if (!(rawItem instanceof Map<?, ?> itemMap)) {
                throw new IllegalStateException("Invalid bundle item at index " + index + ": " + rawItem);
            }

            try {
                ItemStack item = parseRawItem(itemMap);
                if (item == null || item.getType().isAir()) {
                    throw new IllegalStateException("Bundle item at index " + index + " is empty.");
                }
                items.add(item);
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Invalid bundle item at index " + index + ": " + e.getMessage(), e);
            }
        }
        bundleMeta.setItems(items);
    }

    private static void parseContainerContents(List<?> containerList, BlockStateMeta blockStateMeta) {
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            throw new IllegalStateException("Container contents are only supported for shulker boxes.");
        }

        Inventory shulkerInventory = shulkerBox.getInventory();
        for (Object rawSlotObj : containerList) {
            if (!(rawSlotObj instanceof Map<?, ?> slotMap)) {
                throw new IllegalStateException("Invalid shulker-box container entry: " + rawSlotObj);
            }

            if (!(slotMap.get("slot") instanceof Number slotValue) || !(slotMap.get("item") instanceof Map<?, ?> itemMap)) {
                throw new IllegalStateException("Shulker-box container entry must contain a numeric slot and an item map.");
            }

            int slotIdx = slotValue.intValue();
            if (slotIdx < 0 || slotIdx >= shulkerInventory.getSize()) {
                throw new IllegalStateException("Shulker-box item slot is out of range: " + slotIdx);
            }

            try {
                ItemStack storedItem = parseRawItem(itemMap);
                if (storedItem == null || storedItem.getType().isAir()) {
                    throw new IllegalStateException("Shulker-box item at slot " + slotIdx + " is empty.");
                }
                shulkerInventory.setItem(slotIdx, storedItem);
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Invalid shulker-box item at slot " + slotIdx + ": " + e.getMessage(), e);
            }
        }
        blockStateMeta.setBlockState(shulkerBox);
    }

    private static void parseEnchantmentMap(Map<?, ?> enchantMap, ItemMeta meta, boolean stored) {
        if (stored && !(meta instanceof EnchantmentStorageMeta)) {
            throw new IllegalStateException("Stored enchantments are only supported on enchanted books.");
        }
        EnchantmentStorageMeta storageMeta = stored ? (EnchantmentStorageMeta) meta : null;
        for (Map.Entry<?, ?> entry : enchantMap.entrySet()) {
            String enchId = String.valueOf(entry.getKey()).replace("minecraft:", "").toLowerCase();
            if (!(entry.getValue() instanceof Number level)) {
                throw new IllegalStateException("Invalid enchantment level for '" + enchId + "': " + entry.getValue());
            }
            int lvl = level.intValue();

            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchId));
            if (enchantment != null) {
                if (storageMeta != null) {
                    storageMeta.addStoredEnchant(enchantment, lvl, true);
                } else {
                    meta.addEnchant(enchantment, lvl, true);
                }
            } else {
                throw new IllegalStateException("Unsupported enchantment key '" + enchId + "'.");
            }
        }
    }
}