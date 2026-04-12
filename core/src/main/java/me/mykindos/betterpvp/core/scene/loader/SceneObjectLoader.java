package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for loaders that hydrate {@link SceneObject} instances from an external
 * data source (Mapper data-points, database rows, YAML config, etc.).
 * <p>
 * <h3>Lifecycle</h3>
 * Concrete loaders implement {@link #load()} and {@link #unload()}. The framework calls
 * them indirectly through {@link #reload()} - which always unloads before loading - so
 * strategies never call {@link #load()} or {@link #unload()} directly.
 * <p>
 * <h3>Tracking managed objects</h3>
 * Call {@link #spawn(SceneObject, Entity)} (or {@link #spawn(SceneObject, Entity, SceneObjectRegistry)})
 * for every object created inside {@link #load()}. The base class tracks those objects
 * and removes them all automatically when {@link #unload()} is called (before your
 * override runs), so overrides only need to clear non-SceneObject state.
 * <p>
 * <h3>Strategies</h3>
 * Declare which reload triggers apply by overriding {@link #getStrategies()}.
 * The {@link SceneLoaderManager} binds these on registration and unbinds them on shutdown.
 *
 * @see OnEnableLoadStrategy
 * @see ModelEngineLoadStrategy
 */
public abstract class SceneObjectLoader {

    private final List<SceneObject> managed = new ArrayList<>();

    /**
     * Spawns and registers all scene objects for this loader. Called by {@link #reload()}.
     * Use {@link #spawn(SceneObject, Entity, SceneObjectRegistry)} to create objects so
     * they are automatically tracked and cleaned up on the next unload.
     */
    protected abstract void load();

    /**
     * Tears down any non-SceneObject state owned by this loader (timers, extra entities, etc.).
     * All {@link SceneObject} instances tracked via {@link #spawn} are removed before
     * this method is called, so overrides do not need to remove them manually.
     */
    protected void unload() {}

    /**
     * Safely reloads this loader: removes all tracked objects, calls {@link #unload()},
     * then calls {@link #load()}. Strategies should always call this rather than
     * calling {@link #load()} / {@link #unload()} directly.
     */
    public final void reload() {
        doUnload();
        load();
    }

    /**
     * Removes all tracked objects and calls {@link #unload()} without re-loading.
     * Called by {@link SceneLoaderManager} on shutdown.
     */
    final void shutdownLoader() {
        doUnload();
    }

    private void doUnload() {
        managed.forEach(SceneObject::remove);
        managed.clear();
        unload();
    }

    /**
     * Returns the {@link LoadStrategy} instances that determine when this loader reloads.
     * Override to add triggers; the default is an empty list (manual-only reload).
     */
    public List<LoadStrategy> getStrategies() {
        return Collections.emptyList();
    }

    /**
     * Initializes {@code object} with {@code entity}, registers it with {@code registry},
     * and tracks it so it is automatically removed on the next {@link #unload()}.
     *
     * @param object   the uninitialized scene object
     * @param entity   the entity to bind
     * @param registry the registry to register the object with
     * @param <T>      the concrete scene object type
     * @return the same object, now initialized, registered, and tracked
     */
    protected <T extends SceneObject> T spawn(@NotNull T object, @NotNull Entity entity, @NotNull SceneObjectRegistry registry) {
        object.init(entity);
        registry.register(object);
        managed.add(object);
        return object;
    }

    /**
     * Initializes {@code object} with {@code entity} and tracks it without registering it
     * in the global registry. Use this for objects that should be lifecycle-managed by
     * this loader but do not need to be globally discoverable.
     */
    protected <T extends SceneObject> T spawn(@NotNull T object, @NotNull Entity entity) {
        object.init(entity);
        managed.add(object);
        return object;
    }

    /**
     * Tracks a pre-initialized scene object so it is removed on the next {@link #unload()}.
     * Use this when the object was already initialized and registered externally.
     */
    protected <T extends SceneObject> T track(@NotNull T object) {
        managed.add(object);
        return object;
    }

}
