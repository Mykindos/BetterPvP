package me.mykindos.betterpvp.game.framework.model;

/**
 * Represents an entity with a start and stop
 */
public interface Lifecycled {

    /**
     * Called when the entity is being loaded
     */
    void setup();

    /**
     * Called when the entity is being unloaded
     */
    void tearDown();

}
