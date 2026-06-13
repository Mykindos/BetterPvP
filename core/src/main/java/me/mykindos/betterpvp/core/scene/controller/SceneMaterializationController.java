package me.mykindos.betterpvp.core.scene.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Drives the entity lifetime of chunk-managed {@link SceneObject}s off the server's entity-chunk loading.
 * <p>
 * A scene object is a persistent logical actor; its backing entity is a non-persistent materialization that the server
 * discards whenever its chunk's entities unload. This controller closes that gap: it indexes every chunk-managed object
 * by its anchor chunk and (re)spawns it whenever those entities load, and despawns it whenever they unload. The object
 * therefore survives chunk cycling indefinitely without {@code setPersistent(true)} and without holding any chunk
 * loaded - only the handful of objects near players ever hold a real entity, so the scheme scales to thousands of props.
 *
 * <h3>Why {@link EntitiesLoadEvent}, not {@code ChunkLoadEvent}</h3>
 * Since 1.17 Paper separates entity chunks from terrain chunks; {@code ChunkLoadEvent} no longer exposes entities and
 * has had fire-reliability regressions. {@link EntitiesLoadEvent}/{@link EntitiesUnloadEvent} fire precisely at the
 * boundary where entities may (not) exist in a chunk - exactly the lifetime an object's body should track.
 *
 * <p>Registration is automatic: {@link SceneObjectRegistry} calls {@link #manage}/{@link #unmanage} as chunk-managed
 * objects register/unregister, so callers only need {@link SceneObject#configureMaterialization} before registering.
 */
@BPvPListener
@Singleton
@CustomLog
public class SceneMaterializationController implements Listener {

    /** worldUID -> packed-chunk-key -> objects anchored in that chunk. */
    private final Map<UUID, Map<Long, Set<SceneObject>>> index = new HashMap<>();
    private final Core core;

    @Inject
    public SceneMaterializationController(Core core, SceneObjectRegistry registry) {
        this.core = core;
        registry.setMaterializationController(this);
    }

    /**
     * Indexes {@code object} by its anchor chunk and materializes it immediately if that chunk's entities are already
     * loaded. Called by the registry for every chunk-managed object on register.
     */
    public void manage(@NotNull SceneObject object) {
        if (object.getAnchor() == null) {
            return;
        }
        final World world = object.getAnchor().getWorld();
        if (world == null) {
            return;
        }
        final int cx = object.getAnchor().getBlockX() >> 4;
        final int cz = object.getAnchor().getBlockZ() >> 4;
        index.computeIfAbsent(world.getUID(), k -> new HashMap<>())
                .computeIfAbsent(chunkKey(cx, cz), k -> new HashSet<>())
                .add(object);

        // If the chunk's entities are already present (object registered while a player is nearby), show it now.
        if (world.isChunkLoaded(cx, cz) && world.getChunkAt(cx, cz).isEntitiesLoaded()) {
            object.materialize();
        }
    }

    /** Removes {@code object} from the chunk index. Called by the registry on unregister. */
    public void unmanage(@NotNull SceneObject object) {
        if (object.getAnchor() == null) {
            return;
        }
        final World world = object.getAnchor().getWorld();
        if (world == null) {
            return;
        }
        final Map<Long, Set<SceneObject>> byChunk = index.get(world.getUID());
        if (byChunk == null) {
            return;
        }
        final long key = chunkKey(object.getAnchor().getBlockX() >> 4, object.getAnchor().getBlockZ() >> 4);
        final Set<SceneObject> set = byChunk.get(key);
        if (set != null) {
            set.remove(object);
            if (set.isEmpty()) {
                byChunk.remove(key);
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        forEachIn(event.getChunk(), object -> {
            // Spawning inside the load event can race ModelEngine binding; defer one tick and re-check the chunk.
            UtilServer.runTaskLater(core, () -> {
                if (object.isRegistered() && !object.isMaterialized() && event.getChunk().isEntitiesLoaded()) {
                    object.materialize();
                }
            }, 1L);
        });
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        forEachIn(event.getChunk(), SceneObject::dematerialize);
    }

    /** Visits every still-registered object indexed in {@code chunk}, pruning any that have since been removed. */
    private void forEachIn(@NotNull Chunk chunk, @NotNull java.util.function.Consumer<SceneObject> action) {
        final Map<Long, Set<SceneObject>> byChunk = index.get(chunk.getWorld().getUID());
        if (byChunk == null) {
            return;
        }
        final Set<SceneObject> set = byChunk.get(chunkKey(chunk.getX(), chunk.getZ()));
        if (set == null) {
            return;
        }
        for (Iterator<SceneObject> it = set.iterator(); it.hasNext(); ) {
            final SceneObject object = it.next();
            if (!object.isRegistered()) {
                it.remove();
                continue;
            }
            action.accept(object);
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

}
