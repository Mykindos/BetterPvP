package me.mykindos.betterpvp.progression.model.stats;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Represents a manager for {@link ProgressionData} of a {@link ProgressionTree}.
 */
public abstract class StatsRepository<T extends ProgressionTree, K extends ProgressionData<T>> implements ConfigAccessor {

    private final ConcurrentHashMap<UUID, ProgressionData<?>> saveQueue = new ConcurrentHashMap<>();
    private final AsyncLoadingCache<UUID, K> dataCache;
    protected final Database database;
    protected final Progression plugin;
    protected final String tableName;

    protected StatsRepository(Database database, Progression plugin, String tableName) {
        this.database = database;
        this.plugin = plugin;
        this.tableName = tableName;
        this.dataCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .buildAsync((AsyncCacheLoader<? super UUID, K>) ((key, executor) -> loadCompleteDataAsync(key)));
    }

    /**
     * Saves all data in the save queue.
     */
    public final void saveAll() {
        // Save repo-specific data
        saveQueue.forEach((uuid, data) -> data.prepareUpdates(uuid, database, plugin.getDatabasePrefix()));
        // Save experience
        String expStmt = "INSERT INTO " + plugin.getDatabasePrefix() + "exp (gamer, " + tableName + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + tableName + " = VALUES(" + tableName + ");";
        List<Statement> statements = new ArrayList<>();
        saveQueue.forEach((uuid, data) -> statements.add(new Statement(expStmt,
                new StringStatementValue(uuid.toString()),
                new LongStatementValue(data.getExperience()))));
        database.executeBatch(statements, true);
        // Clear the save queue
        saveQueue.clear();
    }

    /**
     * Shuts down the repository.
     */
    public final void shutdown() {
        saveAll();
        dataCache.synchronous().invalidateAll();
    }

    @SneakyThrows
    public final void save(UUID player) {
        final CompletableFuture<K> future = dataCache.getIfPresent(player);
        if (future != null && future.isDone()) {
            saveQueue.put(player, future.get());
        }
    }

    public final void save(OfflinePlayer player) {
        save(player.getUniqueId());
    }

    public final void save(String playerName) {
        save(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    public final CompletableFuture<K> getDataAsync(OfflinePlayer player) {
        return getDataAsync(player.getUniqueId());
    }

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param playerName The name of the player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    public final CompletableFuture<K> getDataAsync(String playerName) {
        return getDataAsync(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Gets the {@link ProgressionData} for the given player.
     * @param player The player to get the data for.
     * @return The {@link ProgressionData} for the given player.
     */
    public final CompletableFuture<K> getDataAsync(UUID player) {
        return dataCache.get(player);
    }

    private CompletableFuture<K> loadCompleteDataAsync(UUID player) {
        return loadDataAsync(player).thenApplyAsync(data -> {
            String expStmt = "SELECT " + tableName + " FROM " + plugin.getDatabasePrefix() + "exp WHERE gamer = ?;";
            final Statement query = new Statement(expStmt, new StringStatementValue(player.toString()));
            final CachedRowSet result = database.executeQuery(query);
            try {
                if (result.next()) {
                    data.setExperience(result.getLong(tableName));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    protected abstract CompletableFuture<K> loadDataAsync(UUID player);

}
