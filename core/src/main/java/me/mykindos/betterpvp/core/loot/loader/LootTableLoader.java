package me.mykindos.betterpvp.core.loot.loader;

import me.mykindos.betterpvp.core.loot.LootTable;

import java.util.Collection;

/**
 * Interface for loading loot tables.
 */
@FunctionalInterface
public interface LootTableLoader {

    /**
     * Loads loot tables.
     * @return The loaded loot tables.
     */
    Collection<LootTable> load();

}
