package me.mykindos.betterpvp.core.stats.filter;

import me.mykindos.betterpvp.core.stats.Leaderboard;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a {@link Leaderboard} that can be filtered
 */
public interface Filtered {

    @NotNull FilterType [] acceptedFilters();

}
