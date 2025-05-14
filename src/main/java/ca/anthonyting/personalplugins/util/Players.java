package ca.anthonyting.personalplugins.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Players {
    private final static Set<OfflinePlayer> cachedOfflinePlayers = Arrays.stream(Bukkit.getOfflinePlayers()).collect(Collectors.toSet());

    public static Set<OfflinePlayer> getOfflinePlayersCached() {
        cachedOfflinePlayers.addAll(Bukkit.getOnlinePlayers());
        return cachedOfflinePlayers;
    }
}
