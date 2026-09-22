package me.mykindos.betterpvp.core.scene;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.controller.SceneMaterializationController;
import me.mykindos.betterpvp.core.scene.npc.HumanNpcInteractController;
import me.mykindos.betterpvp.core.scene.npc.PlayerListPacketController;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for all active {@link SceneObject} instances.
 * <p>
 * Objects are indexed by their numeric {@link SceneObject#getId()} for O(1) lookup.
 * Type-filtered views are available via {@link #getObjects(Class)}.
 * <p>
 * Registration is owned by {@link SceneObjectFactory} and
 * {@link me.mykindos.betterpvp.core.world.content.WorldContentScope}. Unregistration
 * happens automatically when {@link SceneObject#remove()} is called on a registered object.
 */
@Singleton
public final class SceneObjectRegistry {

    private final Map<Integer, SceneObject> objects = new ConcurrentHashMap<>();

    /**
     * Objects by the UUID of their body, and of any extra entity that stands in for them (see {@link #alias}). Read from
     * packet threads, so concurrent.
     */
    private final Map<UUID, SceneObject> byEntity = new ConcurrentHashMap<>();
    private final Map<UUID, SceneObject> byPersistentId = new ConcurrentHashMap<>();

    /**
     * Set once by {@link SceneMaterializationController} on construction. Null only in the brief window before that
     * singleton is created, or on servers where the scene system is unused.
     */
    @Nullable
    private SceneMaterializationController materializationController;

    @Inject
    private SceneObjectRegistry() {
        PacketEvents.getAPI().getEventManager()
                .registerListener(new PlayerListPacketController(this), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager()
                .registerListener(new HumanNpcInteractController(this), PacketListenerPriority.NORMAL);
    }

    /**
     * Sets the controller that drives chunk-managed materialization. Called once by
     * {@link SceneMaterializationController} on construction.
     */
    public void setMaterializationController(@NotNull SceneMaterializationController controller) {
        this.materializationController = controller;
    }

    /**
     * Registers a scene object and back-links this registry so the object can auto-unregister on removal. The object
     * may be dormant (no entity yet) - the PDC marker is stamped by the object itself when it materializes. If the
     * object is {@link SceneObject#isChunkManaged() chunk-managed}, it is handed to the
     * {@link SceneMaterializationController}, which materializes it now (if its chunk is loaded) and re-spawns it across
     * chunk cycles thereafter.
     */
    public void register(@NotNull SceneObject object) {
        object.registry = this;
        objects.put(object.getId(), object);
        if (object.isInitialized()) {
            byEntity.put(object.getEntity().getUniqueId(), object);
        }
        if (object.getPersistentId() != null) {
            byPersistentId.put(object.getPersistentId(), object);
        }
        if (materializationController != null && object.isChunkManaged()) {
            materializationController.manage(object);
        }
    }

    /**
     * Removes a scene object from the registry. The object's back-link is cleared.
     * Prefer calling {@link SceneObject#remove()} instead of this directly.
     */
    public void unregister(@NotNull SceneObject object) {
        objects.remove(object.getId());
        byEntity.values().removeIf(indexed -> indexed == object);
        if (object.getPersistentId() != null) {
            byPersistentId.remove(object.getPersistentId(), object);
        }
        object.registry = null;
        if (materializationController != null && object.isChunkManaged()) {
            materializationController.unmanage(object);
        }
    }

    /** Keeps the entity index in step when a registered object's body changes. */
    void rebind(@NotNull SceneObject object, @Nullable Entity previous, @Nullable Entity next) {
        if (previous != null) {
            byEntity.remove(previous.getUniqueId(), object);
        }
        if (next != null) {
            byEntity.put(next.getUniqueId(), object);
        }
    }

    /**
     * Makes {@code part} resolve to {@code owner}, so interacting with an extra entity (a hitbox, a seat) reaches the
     * object it belongs to. Removed again by {@link #unalias}, or when the owner is unregistered.
     */
    public void alias(@NotNull Entity part, @NotNull SceneObject owner) {
        byEntity.put(part.getUniqueId(), owner);
    }

    public void unalias(@NotNull Entity part) {
        byEntity.remove(part.getUniqueId());
    }

    @Nullable
    public SceneObject getObject(int id) {
        return objects.get(id);
    }

    @Nullable
    public <T extends SceneObject> T getObject(int id, @NotNull Class<T> type) {
        final SceneObject obj = objects.get(id);
        return type.isInstance(obj) ? type.cast(obj) : null;
    }

    @Nullable
    public SceneObject getObject(@NotNull Entity entity) {
        return getObject(entity.getUniqueId());
    }

    /** The object whose body, or one of whose aliased parts, has this entity UUID. */
    @Nullable
    public SceneObject getObject(@NotNull UUID uuid) {
        return byEntity.get(uuid);
    }

    /** The registered object carrying this {@link SceneObject#getPersistentId() persistent id}, if one is standing. */
    @Nullable
    public SceneObject getObjectByPersistentId(@NotNull UUID persistentId) {
        return byPersistentId.get(persistentId);
    }

    @Nullable
    public <T extends SceneObject> T getObject(@NotNull Entity entity, @NotNull Class<T> type) {
        return getObject(entity.getUniqueId(), type);
    }

    @Nullable
    public <T extends SceneObject> T getObject(@NotNull UUID uuid, @NotNull Class<T> type) {
        final SceneObject obj = getObject(uuid);
        return type.isInstance(obj) ? type.cast(obj) : null;
    }

    public Collection<SceneObject> getObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }

    public <T extends SceneObject> Collection<T> getObjects(@NotNull Class<T> type) {
        return objects.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

}
