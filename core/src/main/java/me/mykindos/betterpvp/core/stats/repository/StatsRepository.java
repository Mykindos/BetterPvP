package me.mykindos.betterpvp.core.stats.repository;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class StatsRepository<T extends PlayerData> implements ConfigAccessor {

    protected final ConcurrentHashMap<UUID, T> saveQueue = new ConcurrentHashMap<>();
    protected final AsyncLoadingCache<UUID, T> dataCache;
    protected final Database database;
    protected final BPvPPlugin plugin;
    protected final String tableName;

    protected StatsRepository(BPvPPlugin plugin, String tableName) {
        this.database = plugin.getInjector().getInstance(Database.class);
        this.plugin = plugin;
        this.tableName = tableName;
        this.dataCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .buildAsync((AsyncCacheLoader<? super UUID, T>) ((key, executor) -> loadCompleteDataAsync(key)));
    }

    /**
     * Saves all data in the save queue.
     */
    public final void saveAll(boolean async) {
        UtilServer.runTask(plugin, async, () -> {
            // Save repo-specific data
            saveQueue.forEach((uuid, data) -> data.prepareUpdates(uuid, database, plugin.getDatabasePrefix()));
            // Save experience
            log.info("Saving {} players.", saveQueue.size());
            postSaveAll();
            // Clear the save queue
            saveQueue.clear();
        });
    }

    /**
     * Called after all data is saved, before the save queue is cleared.
     */
    protected abstract void postSaveAll();

    /**
     * Shuts down the repository.
     */
    public final void shutdown() {
        saveAll(false);
        dataCache.synchronous().invalidateAll();
    }

    @SneakyThrows
    public final void saveAsync(UUID player) {
        final CompletableFuture<T> future = dataCache.getIfPresent(player);
        if (future != null && future.isDone()) {
            saveQueue.put(player, future.get());
        }
    }

    public final void saveAsync(OfflinePlayer player) {
        saveAsync(player.getUniqueId());
    }

    public final void saveAsync(String playerName) {
        saveAsync(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Gets the {@link PlayerData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link PlayerData} for the given player.
     */
    public final CompletableFuture<T> getDataAsync(OfflinePlayer player) {
        return getDataAsync(player.getUniqueId());
    }

    /**
     * Gets the {@link PlayerData} for the given player.
     * @param playerName The name of the player to get the data for.
     * @return The {@link PlayerData} for the given player.
     */
    public final CompletableFuture<T> getDataAsync(String playerName) {
        return getDataAsync(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Gets the {@link PlayerData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link PlayerData} for the given player.
     */
    public final CompletableFuture<T> getDataAsync(UUID player) {
        return dataCache.get(player);
    }

    protected CompletableFuture<T> loadCompleteDataAsync(UUID player) {
        if (saveQueue.containsKey(player)) {
            // Return the updated data if it's in the save queue instead of loading new data because this is more up-to-date
            final CompletableFuture<T> saved = CompletableFuture.completedFuture(saveQueue.get(player));
            dataCache.put(player, saved);
            return saved;
        }
        return fetchDataAsync(player).exceptionally(throwable -> {
            log.error("Error loading data for " + player, throwable);
            return null;
        });
    }

    /**
     * Fetches the data for the given player directly from the database.
     *
     * <b>NOTE: Use with caution. This is a direct database call. For common use call {@link StatsRepository#getDataAsync}</b>
     * @param player
     * @return
     */
    public abstract CompletableFuture<T> fetchDataAsync(UUID player);

}