package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.MainPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerCountListener implements Listener {

    private static final MainPlugin main = MainPlugin.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        main.getBackupMaker().setHavePlayersBeenOnline(true);
    }
}
