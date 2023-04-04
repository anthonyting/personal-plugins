package ca.anthonyting.personalplugins.listeners;

import org.bukkit.event.Listener;

abstract public class CancellableListener implements Listener {
    abstract public void cleanup();
}
