package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Backup implements CommandExecutor {

    private final Plugin main = Main.getPlugin();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        String backupDirectoryName = main.getConfig().getString("temp-backup-directory");
        long delay = main.getConfig().getLong("backup-freq");
        if (backupDirectoryName == null || delay < 1) {
            main.getLogger().info("Backups are disabled.");
            return true;
        }
        if (commandSender instanceof Player && !commandSender.hasPermission("forcebackup")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Permission denied."));
            return true;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                Main.getBackupMaker().run();
            }
        }.runTaskAsynchronously(main);
        return true;
    }
}
