package me.mykindos.betterpvp.core.scene.loader;

import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * {@link TrackedLoader} specialisation for {@link SceneObject}s (NPCs, props, displays) sourced from an external data
 * source (Mapper data-points, database rows, YAML config, etc.).
 * <p>
 * Call {@link #spawn(SceneObject, Entity)} (or {@link #spawn(SceneObject, Entity, SceneObjectRegistry)}) for every
 * object created inside {@link #load()}. The base class tracks those objects and removes them all automatically on the
 * next {@link #reload()}/shutdown, so overrides of {@link #unload()} only need to clear non-SceneObject state.
 *
 * @see OnEnableLoadStrategy
 * @see ModelEngineLoadStrategy
 */
public abstract class SceneObjectLoader extends TrackedLoader<SceneObject> {

    @Override
    protected void releaseTracked(@NotNull SceneObject item) {
        item.remove();
    }

    /**
     * Initializes {@code object} with {@code entity}, registers it with {@code registry}, and tracks it so it is
     * automatically removed on the next {@link #unload()}.
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
        return track(object);
    }

    /**
     * Initializes {@code object} with {@code entity} and tracks it without registering it in the global registry. Use
     * this for objects that should be lifecycle-managed by this loader but do not need to be globally discoverable.
     */
    protected <T extends SceneObject> T spawn(@NotNull T object, @NotNull Entity entity) {
        object.init(entity);
        return track(object);
    }

    /**
     * Registers {@code object} as <b>chunk-managed</b>: it is tracked and registered now but stays dormant until its
     * anchor chunk's entities load, at which point {@code SceneMaterializationController} spawns its body via
     * {@code entityFactory} and re-spawns it across chunk cycles. Use this for any object outside a force-loaded chunk
     * so it survives players leaving and returning to render range.
     *
     * @param object        the uninitialized scene object
     * @param anchor        where the object lives
     * @param entityFactory builds the backing entity at the anchor; invoked on every materialization
     * @param registry      the registry to register the object with
     * @param <T>           the concrete scene object type
     * @return the same object, now configured, registered, and tracked
     */
    protected <T extends SceneObject> T spawn(@NotNull T object, @NotNull Location anchor,
                                              @NotNull Function<Location, Entity> entityFactory,
                                              @NotNull SceneObjectRegistry registry) {
        object.configureMaterialization(anchor, entityFactory);
        registry.register(object);
        return track(object);
    }

}
