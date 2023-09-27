package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;

/**
 * Represents a type of fish that can be caught
 */
public interface FishType extends ConfigAccessor {

    /**
     * Get the name of the fish
     * @return The name of the fish
     */
    String getName();

    /**
     * @return The minimum weight of the fish
     */
    int getMinWeight();

    /**
     * @return The maximum weight of the fish
     */
    int getMaxWeight();

    /**
     * @return The frequency of the fish. The higher the frequency, the more common the fish is.
     */
    int getFrequency();

}
