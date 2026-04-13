package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fires once immediately when {@link #bind(SceneObjectLoader, BPvPPlugin)} is called -
 * which happens during plugin enable after Guice injection is complete.
 * <p>
 * Use this for loaders that don't depend on ModelEngine or any async initialization,
 * and only need to load once on startup.
 */
public class OnEnableLoadStrategy implements LoadStrategy {

    @Override
    public void bind(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin) {
        loader.reload();
    }

    @Override
    public void unbind() {
        // No listener to unregister - this strategy fires once and is done.
    }

}
