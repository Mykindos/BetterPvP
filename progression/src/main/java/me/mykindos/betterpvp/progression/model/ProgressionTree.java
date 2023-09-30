package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.stats.StatsRepository;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a progression tree, with skills, buffs, and ways of gaining experience.
 */
public interface ProgressionTree extends ConfigAccessor {

    /**
     * Get the name of the progression tree
     * @return The name of the progression tree
     */
    String getName();

    @Nullable Leaderboard<?> getLeaderboard();

    StatsRepository<?, ?> getStatsRepository();
}