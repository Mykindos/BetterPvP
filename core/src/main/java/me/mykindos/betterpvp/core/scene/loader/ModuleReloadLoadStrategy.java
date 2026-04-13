package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.jetbrains.annotations.NotNull;

/**
 * Reloads the bound {@link SceneObjectLoader} whenever the owning module/plugin is reloaded.
 * <p>
 * This strategy registers itself with {@link BPvPPlugin#getReloadables()} on bind, so it
 * follows the same lifecycle as other {@link Reloadable} hooks.
 */
public class ModuleReloadLoadStrategy implements LoadStrategy, Reloadable {

    private BPvPPlugin plugin;
    private SceneObjectLoader loader;

    @Override
    public void bind(@NotNull SceneObjectLoader loader, @NotNull BPvPPlugin plugin) {
        this.loader = loader;
        this.plugin = plugin;
        plugin.getReloadables().add(this);
    }

    @Override
    public void unbind() {
        if (plugin != null) {
            plugin.getReloadables().remove(this);
        }

        this.loader = null;
        this.plugin = null;
    }

    @Override
    public void reload() {
        if (loader != null) {
            loader.reload();
        }
    }

}
