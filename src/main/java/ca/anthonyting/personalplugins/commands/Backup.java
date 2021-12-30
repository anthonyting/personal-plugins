package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Backup implements CommandExecutor {

    private final MainPlugin main = MainPlugin.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
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

        main.getServer().savePlayers();
        main.getServer().getWorlds().forEach(World::save);
        new BukkitRunnable() {
            @Override
            public void run() {
                // force a backup, but do not interfere with scheduled backups
                if (main.getBackupMaker().havePlayersBeenOnline()) {
                    main.getBackupMaker().run();
                } else {
                    main.getBackupMaker().setHavePlayersBeenOnline(true);
                    main.getBackupMaker().run();
                    main.getBackupMaker().setHavePlayersBeenOnline(false);
                }
            }
        }.runTaskAsynchronously(main);
        return true;
    }
}
