package me.mykindos.betterpvp.core.scene.behavior;

/**
 * A discrete, composable unit of behaviour that can be attached to any
 * {@link me.mykindos.betterpvp.core.scene.SceneEntity} (NPCs, props, etc.).
 * <p>
 * Behaviours are ticked each server tick by
 * {@link me.mykindos.betterpvp.core.scene.controller.SceneTicker}.
 * Multiple behaviours can be stacked on a single entity - they are all ticked independently.
 */
public interface SceneBehavior {

    /**
     * Called every server tick while this behaviour is attached to a scene entity.
     */
    void tick();

    /**
     * Called once when this behaviour is added to an entity via
     * {@link me.mykindos.betterpvp.core.scene.SceneEntity#addBehavior(SceneBehavior)}.
     */
    default void start() {}

    /**
     * Called once when this behaviour is removed from an entity, or when the entity is removed.
     */
    default void stop() {}
}
