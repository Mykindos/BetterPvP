package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import org.jetbrains.annotations.NotNull;

public interface FishingLootType extends ConfigAccessor {

    /**
     * @return The frequency of the fish. The higher the frequency, the more common the fish is.
     */
    int getFrequency();

    /**
     * Generate a loot object
     * @return The loot object
     */
    FishingLoot generateLoot();

    /**
     * Get the name of the fish
     * @return The name of the fish
     */
    @NotNull String getName();


}
