package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.progression.model.stats.ProgressionData;

import java.util.SortedMap;

/**
 * Represents a leaderboard for a {@link ProgressionTree}.
 * @param <T> The type of {@link ProgressionTree}.
 */
public interface Leaderboard<T extends ProgressionTree> {

    /**
     * Gets the top players in the leaderboard.
     * @param amount The amount of players to get.
     * @return The top players in the leaderboard.
     */
    SortedMap<String, ProgressionData<T>> getTop(int amount);

    /**
     * Inserts the given data into the leaderboard.
     * @param data The data to insert.
     */
    void insert(ProgressionData<T> data);

}
