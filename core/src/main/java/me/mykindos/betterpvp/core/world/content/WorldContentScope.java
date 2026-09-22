package me.mykindos.betterpvp.core.world.content;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything one binding has put into one world, so it can be handed back exactly.
 * <p>
 * Content may keep its scope and add to it for as long as the world is loaded. What it adds later is released with
 * the rest, which is what lets something placed by a player mid-session disappear with its world.
 */
@CustomLog
public final class WorldContentScope {

    private final ZoneManager zoneManager;
    private final SceneObjectRegistry sceneRegistry;

    private final List<Zone> zones = new ArrayList<>();
    private final List<SceneObject> objects = new ArrayList<>();
    private final List<Runnable> releases = new ArrayList<>();
    private boolean released;

    WorldContentScope(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry) {
        this.zoneManager = zoneManager;
        this.sceneRegistry = sceneRegistry;
    }

    public void add(@NotNull Zone zone) {
        checkOpen();
        zoneManager.register(zone);
        zones.add(zone);
    }

    /** Registers a chunk-managed object, which spawns its body when its anchor chunk's entities load. */
    public void add(@NotNull SceneSpawn spawn) {
        checkOpen();
        final SceneObject object = spawn.getObject();
        object.configureMaterialization(spawn.getAnchor(), spawn.getEntityFactory());
        sceneRegistry.register(object);
        objects.add(object);
    }

    /**
     * Binds an object to an entity spawned now and registers it. Use {@link #add(SceneSpawn)} instead for anything
     * outside a force-loaded chunk, since an eagerly spawned body does not come back after its chunk unloads.
     */
    public <T extends SceneObject> T spawn(@NotNull T object, @NotNull Entity entity) {
        checkOpen();
        object.init(entity);
        sceneRegistry.register(object);
        objects.add(object);
        return object;
    }

    /** Takes ownership of an object that is already spawned and registered, such as one made by a factory. */
    public <T extends SceneObject> T adopt(@NotNull T object) {
        checkOpen();
        objects.add(object);
        return object;
    }

    /** Runs when this scope is released, before its scene objects and zones are removed. */
    public void onRelease(@NotNull Runnable release) {
        checkOpen();
        releases.add(release);
    }

    public boolean isReleased() {
        return released;
    }

    int objectCount() {
        return objects.size();
    }

    int zoneCount() {
        return zones.size();
    }

    void release() {
        if (released) {
            return;
        }
        released = true;

        for (Runnable release : releases) {
            try {
                release.run();
            } catch (Exception exception) {
                log.error("World content release hook failed", exception).submit();
            }
        }
        objects.forEach(SceneObject::remove);
        zones.forEach(zoneManager::unregister);
    }

    private void checkOpen() {
        if (released) {
            throw new IllegalStateException("World content scope has already been released");
        }
    }
}
