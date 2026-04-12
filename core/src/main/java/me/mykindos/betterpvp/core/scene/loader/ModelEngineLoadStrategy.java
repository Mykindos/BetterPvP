package me.mykindos.betterpvp.core.scene.loader;

import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Reloads the bound {@link SceneObjectLoader} each time ModelEngine finishes its
 * generation pipeline ({@link ModelGenerator.Phase#FINISHED}).
 * <p>
 * This covers both the initial server start and every {@code /meg reload}, ensuring
 * that ModelEngine-dependent content (models, animations) is always in sync with
 * the freshly-loaded model registry.
 * <p>
 * The strategy registers itself as a Bukkit {@link Listener} on {@link #bind} and
 * unregisters on {@link #unbind}, so no manual listener management is required in
 * individual loaders.
 */
public class ModelEngineLoadStrategy implements LoadStrategy, Listener {

    private BPvPPlugin plugin;
    private SceneObjectLoader loader;

    @Override
    public void bind(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin) {
        this.loader = loader;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unbind() {
        HandlerList.unregisterAll(this);
        this.loader = null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onModelEngineFinished(ModelRegistrationEvent event) {
        if (event.getPhase() != ModelGenerator.Phase.FINISHED) {
            return;
        }

        UtilServer.runTask(this.plugin, () -> loader.reload());
    }

}
