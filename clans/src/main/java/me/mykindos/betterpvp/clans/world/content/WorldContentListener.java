package me.mykindos.betterpvp.clans.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Drives {@link WorldContentService} off the server and world lifecycle.
 * <p>
 * The start-up sweep waits for {@link ServerStartEvent} because Mapper data is only readable by then, and because every
 * island declares its content during plugin enable - sweeping earlier would find nothing to install.
 * <p>
 * Runtime worlds are handled per world rather than through {@code WorldLoadStrategy}, which rebuilds every world at
 * once: with instanced islands appearing and disappearing constantly, that would repeatedly tear down content players
 * are standing in.
 */
@BPvPListener
@Singleton
public class WorldContentListener implements Listener {

    private final WorldContentService contentService;

    @Inject
    public WorldContentListener(@NotNull WorldContentService contentService) {
        this.contentService = contentService;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerStart(ServerStartEvent event) {
        contentService.start();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        contentService.loadWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        contentService.unloadWorld(event.getWorld());
    }
}
