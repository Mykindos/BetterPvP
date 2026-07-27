package me.mykindos.betterpvp.core.item.impl.cannon.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonRecord;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonStore;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModelEngineLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader;
import me.mykindos.betterpvp.core.scene.loader.WorldLoadStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Rebuilds every player-placed cannon from {@link CannonStore}, so cannons in unloaded chunks are known about without
 * being spawned and none can be lost because its chunk was cold at boot.
 * <p>
 * Reloads on ModelEngine finalize (models must exist before a cannon can wear one) and whenever a world loads, so
 * cannons in on-demand worlds appear as soon as their world does.
 */
@Singleton
@CustomLog
@PluginAdapter("ModelEngine")
public class CannonStoreLoader extends SceneObjectLoader {

    private final CannonStore store;
    private final CannonService service;

    @Inject
    private CannonStoreLoader(Core core, CannonStore store, CannonService service, SceneLoaderManager loaderManager) {
        this.store = store;
        this.service = service;
        loaderManager.register(this, core);
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ModelEngineLoadStrategy(), new WorldLoadStrategy());
    }

    @Override
    protected void load() {
        int restored = 0;
        for (CannonRecord record : store.all()) {
            final CannonProp prop = service.restore(record);
            if (prop != null) {
                track(prop);
                restored++;
            }
        }
        log.info("Restored {} placed cannon(s) from store", restored).submit();
    }

    /**
     * Releases the in-memory object only; the store is untouched, since a reload must not delete cannons.
     */
    @Override
    protected void releaseTracked(@NotNull SceneObject item) {
        item.remove();
    }
}
