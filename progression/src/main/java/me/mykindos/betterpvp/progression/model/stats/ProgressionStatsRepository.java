package me.mykindos.betterpvp.progression.model.stats;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionTree;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a manager for {@link ProgressionData} of a {@link ProgressionTree}.
 */
@Slf4j
public abstract class ProgressionStatsRepository<T extends ProgressionTree, K extends ProgressionData<T>> extends StatsRepository<K> {

    protected T tree;

    protected ProgressionStatsRepository(Progression plugin, String tableName) {
        super(plugin, tableName);
    }

    protected abstract Class<T> getTreeClass();

    @Override
    protected void postSaveAll() {
        String expStmt = "INSERT INTO " + plugin.getDatabasePrefix() + "exp (Gamer, " + tableName + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + tableName + " = VALUES(" + tableName + ");";
        List<Statement> statements = new ArrayList<>();
        saveQueue.forEach((uuid, data) -> statements.add(new Statement(expStmt,
                new StringStatementValue(uuid.toString()),
                new LongStatementValue(data.getExperience()))));
        database.executeBatch(statements, false);
    }

    @Override
    protected CompletableFuture<K> loadCompleteDataAsync(UUID player) {
        final CompletableFuture<K> loaded = super.loadCompleteDataAsync(player);
        if (saveQueue.containsKey(player)) {
            return loaded; // Don't load their XP if they're already loaded in / about to be saved
        }

        if (tree == null) {
            tree = ((Progression) plugin).getProgressionsManager().fromClass(getTreeClass());
        }

        // Otherwise, these people were just loaded from the database, so load their XP
        return loaded.thenApplyAsync(data -> {
            String expStmt = "SELECT " + tableName + " FROM " + plugin.getDatabasePrefix() + "exp WHERE gamer = ?;";
            final Statement query = new Statement(expStmt, new StringStatementValue(player.toString()));
            final CachedRowSet result = database.executeQuery(query);
            try {
                if (result.next()) {
                    data.setExperience(result.getLong(tableName));
                }
            } catch (Exception e) {
                log.error("Error loading XP for " + player, e);
            }
            data.setTree(tree);
            return data;
        });
    }

}
