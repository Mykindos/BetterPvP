package me.mykindos.betterpvp.core.scene;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.scene.npc.PlayerListPacketController;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
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
 * {@link me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader}; unregistration
 * happens automatically when {@link SceneObject#remove()} is called on a registered object.
 */
@Singleton
public final class SceneObjectRegistry {

    private final Map<Integer, SceneObject> objects = new ConcurrentHashMap<>();

    @Inject
    private SceneObjectRegistry() {
        PacketEvents.getAPI().getEventManager()
                .registerListener(new PlayerListPacketController(this), PacketListenerPriority.NORMAL);
    }

    /**
     * Registers a scene object. Stamps the NPC PDC marker on its entity and
     * back-links this registry so the object can auto-unregister on removal.
     */
    public void register(@NotNull SceneObject object) {
        object.registry = this;
        object.getEntity().getPersistentDataContainer()
                .set(CoreNamespaceKeys.SCENE_OBJECT, PersistentDataType.BOOLEAN, true);
        objects.put(object.getId(), object);
    }

    /**
     * Removes a scene object from the registry. The object's back-link is cleared.
     * Prefer calling {@link SceneObject#remove()} instead of this directly.
     */
    public void unregister(@NotNull SceneObject object) {
        objects.remove(object.getId());
        object.registry = null;
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

    @Nullable
    public SceneObject getObject(@NotNull UUID uuid) {
        for (SceneObject obj : objects.values()) {
            if (obj.getEntity().getUniqueId().equals(uuid)) {
                return obj;
            }
        }
        return null;
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
