package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reloads the bound {@link TrackedLoader} whenever a world loads ({@link WorldLoadEvent}), optionally filtered to a
 * single world name.
 * <p>
 * {@link ServerStartLoadStrategy} fires once, at startup, for worlds that are already loaded then. But content can live
 * in a world that is loaded <em>later</em> - on demand, by a teleport flow, or by an external plugin - in which case the
 * one-shot server-start pass runs before the world's Mapper data is even readable and silently finds nothing. This
 * strategy closes that gap: it re-runs the loader the moment its world appears.
 * <p>
 * Idempotent with the other strategies: {@link TrackedLoader#reload()} always releases the previous set before
 * re-loading, so a world that happens to be loaded at server-start (firing both strategies) is handled cleanly.
 */
public class WorldLoadStrategy implements LoadStrategy, Listener {

    @Nullable
    private final String worldName;
    private TrackedLoader<?> loader;

    /** Fires for any world load. Use for world-agnostic loaders that scan every world. */
    public WorldLoadStrategy() {
        this(null);
    }

    /**
     * Fires only when the named world loads.
     *
     * @param worldName the world to watch, or {@code null} for any world
     */
    public WorldLoadStrategy(@Nullable String worldName) {
        this.worldName = worldName;
    }

    @Override
    public void bind(@NotNull TrackedLoader<?> loader, @NotNull BPvPPlugin plugin) {
        this.loader = loader;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unbind() {
        HandlerList.unregisterAll(this);
        this.loader = null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(WorldLoadEvent event) {
        if (worldName != null && !event.getWorld().getName().equals(worldName)) {
            return;
        }
        loader.reload();
    }

}
