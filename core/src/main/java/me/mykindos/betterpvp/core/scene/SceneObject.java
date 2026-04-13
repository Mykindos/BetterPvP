package me.mykindos.betterpvp.core.scene;

import lombok.Getter;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for all server-managed world objects (NPCs, props, text displays, etc.).
 * <p>
 * Construction is decoupled from entity creation via a two-phase init pattern:
 * subclasses are constructed first, then {@link #init(Entity)} is called once the
 * backing entity is available. This allows factories and loaders to create entities
 * asynchronously or via APIs (e.g. ModelEngine dummy entities) before binding them.
 * <p>
 * Registration is handled by {@link SceneObjectRegistry}. An object only unregisters
 * itself on {@link #remove()} if it was previously registered.
 */
@Getter
public abstract class SceneObject {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id;
    @Nullable protected Entity entity;

    // Set by SceneObjectRegistry on register(); cleared on unregister().
    // Package-private so only the registry can write it.
    @Nullable SceneObjectRegistry registry;

    protected SceneObject() {
        this.id = COUNTER.getAndIncrement();
    }

    /**
     * Binds the backing entity to this object. Must be called exactly once, before
     * the object is used. Calling this a second time throws {@link IllegalStateException}.
     *
     * @param entity the entity that represents this object in the world
     */
    public final void init(@NotNull Entity entity) {
        if (this.entity != null) {
            throw new IllegalStateException("SceneObject #" + id + " has already been initialized");
        }
        this.entity = entity;
        onInit();
    }

    /**
     * Hook invoked immediately after the entity is bound via {@link #init(Entity)}.
     * Override to perform any setup that requires the entity to be present.
     */
    protected void onInit() {}

    /**
     * Returns the backing entity.
     *
     * @throws IllegalStateException if {@link #init(Entity)} has not been called yet
     */
    @NotNull
    public Entity getEntity() {
        if (entity == null) {
            throw new IllegalStateException("SceneObject #" + id + " has not been initialized yet");
        }
        return entity;
    }

    public boolean isInitialized() {
        return entity != null;
    }

    public boolean isRegistered() {
        return registry != null;
    }

    /**
     * Removes this object from the world. If it was registered, it is unregistered first.
     * Subclasses should call {@code super.remove()} after their own cleanup.
     */
    public void remove() {
        if (registry != null) {
            registry.unregister(this);
        }
        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

}
