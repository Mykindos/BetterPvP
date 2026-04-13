package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Loads the bound {@link SceneObjectLoader} once when the server finishes starting
 * ({@link ServerStartEvent}).
 * <p>
 * Use this for loaders that depend on the world and Mapper data being ready, but have
 * <em>no</em> dependency on ModelEngine. Fires later than {@link OnEnableLoadStrategy}
 * (which fires per-plugin during enable) and earlier than {@link ModelEngineLoadStrategy}
 * (which fires when ME finishes its pipeline), making it the right trigger for world-level
 * content that doesn't use custom models.
 * <p>
 * Unlike {@link ModelEngineLoadStrategy}, this strategy fires only once and does not
 * re-fire on {@code /meg reload} or other runtime events.
 */
public class ServerStartLoadStrategy implements LoadStrategy, Listener {

    private SceneObjectLoader loader;

    @Override
    public void bind(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin) {
        this.loader = loader;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unbind() {
        HandlerList.unregisterAll(this);
        this.loader = null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerStart(ServerStartEvent event) {
        loader.reload();
    }

}
