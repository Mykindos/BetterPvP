package me.mykindos.betterpvp.core.scene.loader;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the full lifecycle of all registered {@link SceneObjectLoader} instances.
 * <p>
 * When a loader is registered via {@link #register(SceneObjectLoader, Plugin)}, its
 * declared {@link LoadStrategy} instances are bound immediately. Each strategy then
 * fires on its own trigger (plugin enable, ModelEngine finalize, etc.) and calls
 * {@link SceneObjectLoader#reload()} automatically.
 * <p>
 * On server shutdown, call {@link #shutdown()} to unload all content and unbind all
 * strategy listeners in registration order.
 */
@CustomLog
@Singleton
public class SceneLoaderManager {

    private final Map<SceneObjectLoader, List<LoadStrategy>> loaders = new LinkedHashMap<>();

    /**
     * Registers a loader and immediately binds all of its {@link LoadStrategy} instances.
     * Strategies that fire on bind (e.g. {@link OnEnableLoadStrategy}) will trigger
     * {@link SceneObjectLoader#reload()} before this method returns.
     *
     * @param loader the loader to register
     * @param plugin the owning plugin passed to each strategy for listener registration
     */
    public void register(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin) {
        final List<LoadStrategy> strategies = new ArrayList<>(loader.getStrategies());
        loaders.put(loader, strategies);
        log.info("Registering scene loader {} with {} strategy/ies",
                loader.getClass().getSimpleName(), strategies.size()).submit();
        for (LoadStrategy strategy : strategies) {
            strategy.bind(loader, plugin);
        }
    }

    /**
     * Unloads all registered loaders and unbinds all strategies. Call this during
     * plugin shutdown (e.g. {@code onDisable}).
     */
    public void shutdown() {
        loaders.forEach((loader, strategies) -> {
            strategies.forEach(LoadStrategy::unbind);
            try {
                loader.shutdownLoader();
            } catch (Exception e) {
                log.error("Error unloading scene loader {}", loader.getClass().getSimpleName(), e).submit();
            }
        });
        loaders.clear();
    }

}
