package me.mykindos.betterpvp.core.scene.loader;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base for loaders that hydrate resources of some type {@code T} from an external data source (Mapper data-points,
 * database rows, YAML config, ...) and need a tracked, strategy-driven reload lifecycle.
 * <p>
 * <h3>Lifecycle</h3>
 * Concrete loaders implement {@link #load()} (create resources, tracking each via {@link #track(Object)}) and
 * {@link #releaseTracked(Object)} (how to dispose one tracked resource). The framework drives them through
 * {@link #reload()} - which always releases every tracked resource and runs {@link #unload()} before loading again -
 * so {@link LoadStrategy strategies} never call {@link #load()} or {@link #unload()} directly.
 * <p>
 * <h3>Strategies</h3>
 * Declare which reload triggers apply by overriding {@link #getStrategies()}. The {@link SceneLoaderManager} binds
 * these on registration and unbinds them on shutdown. The same strategies ({@link OnEnableLoadStrategy},
 * {@link ServerStartLoadStrategy}, {@link ModuleReloadLoadStrategy}, ...) work for any subtype, because they only ever
 * call {@link #reload()}.
 *
 * @param <T> the type of resource this loader creates and tracks
 * @see SceneObjectLoader
 * @see me.mykindos.betterpvp.core.world.zone.ZoneLoader
 */
public abstract class TrackedLoader<T> {

    private final List<T> managed = new ArrayList<>();

    /**
     * Creates and {@link #track(Object) tracks} all resources for this loader. Called by {@link #reload()} after the
     * previous set has been released.
     */
    protected abstract void load();

    /**
     * Disposes a single tracked resource (e.g. remove an entity, unregister a zone). Called for every tracked resource
     * before {@link #unload()} runs.
     *
     * @param item the resource to dispose
     */
    protected abstract void releaseTracked(@NotNull T item);

    /**
     * Tears down any non-tracked state owned by this loader (timers, extra listeners, ...). Every resource tracked via
     * {@link #track(Object)} is released before this method is called, so overrides do not need to dispose them.
     */
    protected void unload() {
    }

    /**
     * Safely reloads this loader: releases all tracked resources, calls {@link #unload()}, then calls {@link #load()}.
     * Strategies should always call this rather than {@link #load()} / {@link #unload()} directly.
     */
    public final void reload() {
        doUnload();
        load();
    }

    /**
     * Releases all tracked resources and calls {@link #unload()} without re-loading. Called by
     * {@link SceneLoaderManager} on shutdown.
     */
    final void shutdownLoader() {
        doUnload();
    }

    private void doUnload() {
        managed.forEach(this::releaseTracked);
        managed.clear();
        unload();
    }

    /**
     * Returns the {@link LoadStrategy} instances that determine when this loader reloads. Override to add triggers; the
     * default is an empty list (manual-only reload).
     */
    public List<LoadStrategy> getStrategies() {
        return Collections.emptyList();
    }

    /**
     * Tracks a resource so it is released on the next {@link #unload()}.
     *
     * @param item the resource to track
     * @param <R>  the concrete resource type
     * @return the same resource, now tracked
     */
    protected <R extends T> R track(@NotNull R item) {
        managed.add(item);
        return item;
    }

}
