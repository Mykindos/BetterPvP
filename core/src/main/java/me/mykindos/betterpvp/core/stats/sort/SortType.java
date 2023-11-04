package me.mykindos.betterpvp.core.stats.sort;

import me.mykindos.betterpvp.core.stats.Leaderboard;
import org.jetbrains.annotations.NotNull;

/**
 * Defines parameters to sort the search of a {@link Leaderboard}
 */
public interface SortType {

    @NotNull String getName();

    default boolean accepts(Object entry) {
        return true;
    }
}
