package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.model.stats.StatsRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a progression tree, with skills, buffs, and ways of gaining experience.
 */
public interface ProgressionTree extends ConfigAccessor {

    /**
     * Get the name of the progression tree
     *
     * @return The name of the progression tree
     */
    @NotNull String getName();

    @NotNull StatsRepository<? extends ProgressionTree, ? extends ProgressionData<?>> getStatsRepository();
}