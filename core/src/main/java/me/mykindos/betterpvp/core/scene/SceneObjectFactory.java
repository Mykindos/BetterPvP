package me.mykindos.betterpvp.core.scene;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract factory for command-spawnable {@link SceneObject} types.
 * <p>
 * Factories are registered with {@link SceneObjectFactoryManager} and are invoked
 * by the {@code /npc spawn} (and similar) commands. Every object spawned through
 * a factory is automatically initialized and registered with {@link SceneObjectRegistry}.
 * <p>
 * For data-point / world-load driven spawning, use
 * {@link me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader} instead.
 *
 * @see me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader
 */
@Getter
public abstract class SceneObjectFactory {

    private final String name;
    protected final SceneObjectRegistry registry;

    protected SceneObjectFactory(@NotNull String name, @NotNull SceneObjectRegistry registry) {
        this.name = name;
        this.registry = registry;
    }

    /**
     * Returns the type identifiers this factory recognizes (used by commands for tab-complete).
     */
    public abstract String[] getTypes();

    /**
     * Spawns a scene object of the given type at the given location and registers it.
     *
     * @param location the spawn location
     * @param type     the type string (must be one of {@link #getTypes()})
     * @return the newly spawned, registered scene object
     */
    public abstract SceneObject spawnDefault(@NotNull Location location, @NotNull String type);

    /**
     * Initializes {@code object} with {@code entity} and registers it with the registry.
     * Use this in {@link #spawnDefault} implementations to ensure the two-phase init
     * and registration always happen together in the correct order.
     *
     * @param object the uninitialized scene object
     * @param entity the entity to bind
     * @param <T>    the concrete scene object type
     * @return the same object, now initialized and registered
     */
    protected <T extends SceneObject> T spawn(@NotNull T object, @NotNull Entity entity) {
        object.init(entity);
        registry.register(object);
        return object;
    }

}
