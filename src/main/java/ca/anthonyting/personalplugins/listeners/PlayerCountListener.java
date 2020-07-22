package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerCountListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Main.getBackupMaker().setHavePlayersBeenOnline(true);
        Main.getBackupMaker().setPlayerCountZero(false);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Main.getBackupMaker().setPlayerCountZero(e.getPlayer().getServer().getOnlinePlayers().size() - 1 <= 0);
    }
}
