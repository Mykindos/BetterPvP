package me.mykindos.betterpvp.core.item;

/**
 * Represents a bootstrap for an item, it will register items, and the ItemRegistry will
 * depend on it to start up.
 */
@FunctionalInterface
public interface ItemBootstrap {

    /**
     * Register items for the item registry.
     */
    void registerItems();

}
