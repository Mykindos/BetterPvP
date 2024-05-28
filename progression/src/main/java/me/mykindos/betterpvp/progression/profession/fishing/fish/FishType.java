package me.mykindos.betterpvp.progression.profession.fishing.fish;

import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;

/**
 * Represents a type of fish that can be caught
 */
public interface FishType extends FishingLootType {

    /**
     * @return The minimum weight of the fish
     */
    int getMinWeight();

    /**
     * @return The maximum weight of the fish
     */
    int getMaxWeight();

}
