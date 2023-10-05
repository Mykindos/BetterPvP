package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.tree.fishing.fish.FishType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of fishing rod.
 * Different types of fishing rods are <i>required</i> to catch some {@link FishType}s.
 */
public interface FishingRodType extends ConfigAccessor {

    /**
     * A unique ID for the fishing rod type, DO NOT CHANGE
     * @return The ID of the fishing rod type
     */
    int getId();

    /**
     * @return The name of the fishing rod type
     */
    @NotNull String getName();

    /**
     * Determines if the fishing rod can reel in the loot
     * @param loot The loot
     * @return {@code true} if the fishing rod can reel in the loot, {@code false} otherwise
     */
    boolean canReel(@NotNull FishingLoot loot);

}
