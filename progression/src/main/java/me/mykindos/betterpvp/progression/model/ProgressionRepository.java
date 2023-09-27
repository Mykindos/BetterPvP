package me.mykindos.betterpvp.progression.model;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a manager for {@link ProgressionData} of a {@link ProgressionTree}.
 */
public interface ProgressionRepository<T extends ProgressionTree, K extends ProgressionData<T>> {

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    default CompletableFuture<K> getData(OfflinePlayer player) {
        return getData(player.getUniqueId());
    }

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param playerName The name of the player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    default CompletableFuture<K> getData(String playerName) {
        return getData(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    CompletableFuture<K> getData(UUID player);

    /**
     * Loads the {@link ProgressionData} for the given player from the database, or creates it if it doesn't exist.
     * @param player The player to load or create the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    CompletableFuture<K> loadOrCreate(UUID player);

    /**
     * Saves the given player to the database.
     */
    void save(UUID player);

    /**
     * Saves all data in the repository.
     */
    void save();

    /**
     * Shuts down the repository.
     */
    void shutdown();

}
