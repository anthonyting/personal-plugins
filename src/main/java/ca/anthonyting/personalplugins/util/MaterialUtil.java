package ca.anthonyting.personalplugins.util;

import org.bukkit.Material;

import java.util.Map;

import static java.util.Map.entry;

public class MaterialUtil {
    private static final Map<Material, String> MATERIAL_NAMES = Map.ofEntries(
            entry(Material.SPLASH_POTION, "Splash Potion"),
            entry(Material.LINGERING_POTION, "Lingering Potion"),
            entry(Material.TNT, "TNT"),
            entry(Material.TNT_MINECART, "TNT Minecart"),
            entry(Material.FIRE_CHARGE, "Fire Charge"),
            entry(Material.FLINT_AND_STEEL, "Flint and Steel"),
            entry(Material.LAVA_BUCKET, "Lava Bucket"),
            entry(Material.PUFFERFISH, "Pufferfish"),
            entry(Material.PUFFERFISH_BUCKET, "Pufferfish Bucket"),
            entry(Material.SUSPICIOUS_STEW, "Suspicious Stew"),
            entry(Material.GUNPOWDER, "Gunpowder"),
            entry(Material.SHULKER_BOX, "Shulker Box"),
            entry(Material.BLACK_SHULKER_BOX, "Shulker Box"),
            entry(Material.BLUE_SHULKER_BOX, "Shulker Box"),
            entry(Material.BROWN_SHULKER_BOX, "Shulker Box"),
            entry(Material.CYAN_SHULKER_BOX, "Shulker Box"),
            entry(Material.GRAY_SHULKER_BOX, "Shulker Box"),
            entry(Material.GREEN_SHULKER_BOX, "Shulker Box"),
            entry(Material.LIGHT_BLUE_SHULKER_BOX, "Shulker Box"),
            entry(Material.LIGHT_GRAY_SHULKER_BOX, "Shulker Box"),
            entry(Material.LIME_SHULKER_BOX, "Shulker Box"),
            entry(Material.MAGENTA_SHULKER_BOX, "Shulker Box"),
            entry(Material.ORANGE_SHULKER_BOX, "Shulker Box"),
            entry(Material.PINK_SHULKER_BOX, "Shulker Box"),
            entry(Material.PURPLE_SHULKER_BOX, "Shulker Box"),
            entry(Material.RED_SHULKER_BOX, "Shulker Box"),
            entry(Material.WHITE_SHULKER_BOX, "Shulker Box"),
            entry(Material.YELLOW_SHULKER_BOX, "Shulker Box")
    );

    public static String getFriendlyName(Material material) {
        return MATERIAL_NAMES.getOrDefault(material, material.name().toLowerCase().replace('_', ' '));
    }

    public static boolean isShulkerBox(Material material) {
        return material.name().endsWith("SHULKER_BOX");
    }
}
