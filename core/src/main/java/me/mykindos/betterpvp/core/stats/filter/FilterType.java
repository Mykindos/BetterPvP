package me.mykindos.betterpvp.core.stats.filter;

import me.mykindos.betterpvp.core.stats.Leaderboard;
import org.jetbrains.annotations.NotNull;

/**
 * Defines parameters to reduce the search of a {@link Leaderboard}
 */
public interface FilterType {

    @NotNull String getName();

    default boolean accepts(Object entry) {
        return true;
    }
}
