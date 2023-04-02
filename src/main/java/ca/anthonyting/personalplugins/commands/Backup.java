package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Backup implements CommandExecutor {

    private final MainPlugin main = MainPlugin.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        if (commandSender instanceof Player && !commandSender.hasPermission("personalplugins.forcebackup")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Permission denied."));
            return true;
        }

        String backupDirectoryName = main.getConfig().getString("temp-backup-directory");
        long delay = main.getConfig().getLong("backup-freq");
        if (backupDirectoryName == null || delay < 1) {
            main.getLogger().info("Backups are disabled.");
            return true;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                main.getBackupMaker().run();
            }
        }.runTaskAsynchronously(main);

        return true;
    }
}
