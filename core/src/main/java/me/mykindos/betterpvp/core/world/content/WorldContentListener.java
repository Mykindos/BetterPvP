package me.mykindos.betterpvp.core.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Drives {@link WorldContentService} off the server, world and plugin lifecycle.
 * <p>
 * The start-up sweep waits for {@link ServerStartEvent} because Mapper data is only readable by then, and because every
 * plugin declares its content during enable, so sweeping earlier would find nothing to install.
 */
@BPvPListener
@Singleton
@PluginAdapter("Mapper")
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        if (event.getPlugin() instanceof BPvPPlugin plugin) {
            contentService.release(plugin);
        }
    }
}
