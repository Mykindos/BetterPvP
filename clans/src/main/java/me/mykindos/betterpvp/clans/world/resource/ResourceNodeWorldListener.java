package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps resource nodes in sync with worlds appearing and disappearing at runtime — a discovery island cloned from a
 * template world after server start, for example. Deliberately does not use {@code WorldLoadStrategy}: that strategy
 * calls {@link ResourceNodeLoader#reload()}, which tears down and rebuilds every node in every loaded world (wiping
 * mid-respawn ore/tree state along the way). This listener instead calls {@link ResourceNodeLoader#loadWorld(org.bukkit.World)}
 * / {@link ResourceNodeLoader#unloadWorld(org.bukkit.World)}, which are scoped to the one world that changed.
 */
@BPvPListener
@Singleton
public class ResourceNodeWorldListener implements Listener {

    private final ResourceNodeLoader loader;

    @Inject
    public ResourceNodeWorldListener(@NotNull ResourceNodeLoader loader) {
        this.loader = loader;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        loader.loadWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        loader.unloadWorld(event.getWorld());
    }

}
