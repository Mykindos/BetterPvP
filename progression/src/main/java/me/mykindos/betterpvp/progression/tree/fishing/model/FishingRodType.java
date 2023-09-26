package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of fishing rod.
 * Different types of fishing rods are <i>required</i> to catch some {@link FishType}s.
 */
public interface FishingRodType extends ConfigAccessor {

    /**
     * @return The name of the fishing rod type
     */
    @NotNull String getName();

    /**
     * Determines if the fishing rod can reel in the fish
     * @param fish The fish to reel in
     * @return {@code true} if the fishing rod can reel in the fish, {@code false} otherwise
     */
    boolean canReel(@NotNull Fish fish);

}
