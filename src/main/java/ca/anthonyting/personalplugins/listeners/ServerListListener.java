package ca.anthonyting.personalplugins.listeners;

import ca.anthonyting.personalplugins.Main;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.win32.W32APIOptions;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

interface CLibrary extends Library {
    CLibrary INSTANCE = Native.load((Platform.isWindows() ? "kernel32" : "c"), CLibrary.class, W32APIOptions.DEFAULT_OPTIONS);
    boolean SetConsoleTitle(String title);
    int GetConsoleTitle(char[] lpConsoleTitle, int nSize);
}

public class ServerListListener implements Listener {
    private CLibrary cLib = CLibrary.INSTANCE;
    private final JavaPlugin plugin = Main.getPlugin();
    private String origTitle;
    private String renamedTitle = plugin.getConfig().getString("renamed-console-title");

    public String getOrigTitle() {
        return origTitle;
    }

    public boolean setTitle(String title) {
        return cLib.SetConsoleTitle(title);
    }

    public ServerListListener() {
        int MAX_CHAR_LENGTH = plugin.getConfig().getInt("max-console-title-length");
        char[] titleBuffer = new char[MAX_CHAR_LENGTH];
        if (cLib.GetConsoleTitle(titleBuffer, 50) == 0) {
            plugin.getLogger().warning("Error getting current console title!");
        }
        origTitle = Native.toString(titleBuffer);
        setInitialTitle();
    }

    private void setInitialTitle() {
        if (!updateTitle(plugin.getServer().getOnlinePlayers().size(), plugin.getServer().getMaxPlayers())) {
            plugin.getLogger().warning("Setting initial title failed!");
        }
    }

    private boolean updateTitle(int online, int max) {
        // Reset title to original title of there are no players online, otherwise add the player count.
        String base;
        if (renamedTitle != null) {
            base = renamedTitle;
        } else {
            base = origTitle;
        }
        return online!=0 ? setTitle(base + ": " + online + "/" + max) : setTitle(base);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Server currentServer = e.getPlayer().getServer();
        if (!updateTitle(currentServer.getOnlinePlayers().size(), currentServer.getMaxPlayers())) {
            plugin.getLogger().warning("Updating title on joining failed!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Server currentServer = e.getPlayer().getServer();
        if (!updateTitle(currentServer.getOnlinePlayers().size()-1, currentServer.getMaxPlayers())) {
            // Note: getOnlinePlayers() returns the number of players BEFORE the player leaves, so 1 must be subtracted
            //       to get the number of players AFTER leaving.
            plugin.getLogger().warning("Updating title on quitting failed!");
        }
    }
}
