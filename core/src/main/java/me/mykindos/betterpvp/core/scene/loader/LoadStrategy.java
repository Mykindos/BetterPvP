package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a trigger condition under which a {@link SceneObjectLoader} will (re)load.
 * <p>
 * Each strategy is responsible for registering its own event listener (or equivalent)
 * and calling {@link SceneObjectLoader#reload()} when its trigger fires. This means
 * individual loaders never need to wire their own reload listeners - they simply declare
 * which strategies apply.
 * <p>
 * Implementations must be stateful: they hold the loader and plugin references set
 * during {@link #bind(SceneObjectLoader, BPvPPlugin)} and release them in {@link #unbind()}.
 *
 * <h3>Built-in strategies</h3>
 * <ul>
 *   <li>{@link OnEnableLoadStrategy} - loads once immediately when the plugin enables</li>
 *   <li>{@link ModuleReloadLoadStrategy} - reloads whenever the owning module reloads</li>
 *   <li>{@link ModelEngineLoadStrategy} - reloads whenever ModelEngine finishes its pipeline</li>
 * </ul>
 */
public interface LoadStrategy {

    /**
     * Activates this strategy. Called by {@link SceneLoaderManager} after the plugin
     * and loader are both fully initialized. Implementations should register their
     * event listener (or similar trigger) here.
     *
     * @param loader the loader to call {@link SceneObjectLoader#reload()} on
     * @param plugin the owning plugin (used for Bukkit listener registration)
     */
    void bind(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin);

    /**
     * Deactivates this strategy. Called on shutdown or when the loader is removed.
     * Implementations must unregister any listeners registered in {@link #bind}.
     */
    void unbind();

}
