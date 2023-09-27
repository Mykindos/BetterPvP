package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;

/**
 * Represents a progression tree, with skills, buffs, and ways of gaining experience.
 */
public interface ProgressionTree extends ConfigAccessor {

    /**
     * Get the name of the progression tree
     * @return The name of the progression tree
     */
    String getName();

    /**
     * Get how much experience is required to a level
     * @param level The level to get the experience required for
     * @return The amount of experience required.
     */
    default int getXpRequired(int level) {
        return (int) Math.floor(Math.cbrt(level ^ 4) + (double) (50 * level) / 4);
    }

    <T extends ProgressionTree> Leaderboard<T> getLeaderboard();

}