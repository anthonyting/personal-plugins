package ca.anthonyting.personalplugins.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class LocalDifficultyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        var location = player.getLocation();
        var position = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        // See https://minecraft.wiki/w/Difficulty#Regional_difficulty; Paper's DifficultyInstance supplies both values.
        DifficultyInstance difficulty = ((CraftWorld) player.getWorld()).getHandle().getCurrentDifficultyAt(position);

        player.sendMessage(String.format(Locale.ENGLISH,
                "Local difficulty: %.2f (%.2f clamped multiplier)",
                difficulty.getEffectiveDifficulty(), difficulty.getSpecialMultiplier()));
        return true;
    }
}
