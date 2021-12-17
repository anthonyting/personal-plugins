package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerCountListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Main.getBackupMaker().setHavePlayersBeenOnline(true);
    }
}
