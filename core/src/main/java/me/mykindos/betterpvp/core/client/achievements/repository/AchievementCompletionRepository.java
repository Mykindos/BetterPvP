package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.rowset.CachedRowSet;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

@Singleton
@CustomLog
public class AchievementCompletionRepository implements IRepository<AchievementCompletion> {

    private final Database database;

    @Inject
    public AchievementCompletionRepository(Database database) {
        this.database = database;
    }

    private static TargetDatabase getTargetDatabase(PropertyContainer container) {
        return container instanceof Client ? TargetDatabase.GLOBAL : TargetDatabase.LOCAL;
    }

    public CompletableFuture<Void> save(PropertyContainer container, AchievementCompletion object) {
            final TargetDatabase targetDatabase = getTargetDatabase(container);

            final String globalUpdate = "INSERT INTO global_achievement_completions (Id, User, Namespace, Keyname, Timestamp) VALUES (?, ?, ?, ?, ?);";
            final String localUpdate = "INSERT INTO local_achievement_completions (Id, User, Namespace, Keyname, Timestamp) VALUES (?, ?, ?, ?, ?);";
            final Statement updateStatement = new Statement(targetDatabase == TargetDatabase.GLOBAL ? globalUpdate : localUpdate,
                    new UuidStatementValue(object.getId()),
                    new UuidStatementValue(object.getUser()),
                    new StringStatementValue(object.getKey().getNamespace()),
                    new StringStatementValue(object.getKey().getKey()),
                    new TimestampStatementValue(object.getTimestamp())
            );

            return database.executeUpdate(updateStatement, targetDatabase);
    }

    @NotNull
    public CompletableFuture<ConcurrentHashMap<NamespacedKey, AchievementCompletion>> loadForContainer(@NotNull PropertyContainer container) {
        return CompletableFuture.supplyAsync(() -> {
                    final ConcurrentHashMap<NamespacedKey, AchievementCompletion> completions = new ConcurrentHashMap<>();

                    final TargetDatabase targetDatabase = getTargetDatabase(container);

                    Statement queryStatement = getStatement(container, targetDatabase);

                    try (final CachedRowSet results = database.executeQuery(queryStatement, targetDatabase).join()) {
                        while (results.next()) {
                            final UUID completionId = UUID.fromString(results.getString(1));
                            final UUID user = UUID.fromString(results.getString(2));
                            final String namespace = results.getString(3);
                            final String key = results.getString(4);
                            final Timestamp timestamp = results.getTimestamp(5);

                            final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                    user,
                                    new NamespacedKey(namespace, key),
                                    timestamp
                            );

                            completions.put(achievementCompletion.getKey(), achievementCompletion);
                        }
                    } catch (SQLException e) {
                        log.error("Error getting AchievementCompletions for {} {}: ", container.getClass().getSimpleName(), container.getUniqueId(), e).submit();
                    }

                    loadCompletionRanks(container, completions);

                    return completions;
                }
        );
    }

    /**
     * Loads the {@link AchievementCompletion#getCompletedRank()} for each achievement
     * @param container the {@link PropertyContainer}
     * @param completions the {@link ConcurrentHashMap} to be modified in place
     */
    public void loadCompletionRanks(PropertyContainer container, ConcurrentHashMap<NamespacedKey, AchievementCompletion> completions) {
        final TargetDatabase targetDatabase = getTargetDatabase(container);
        final String globalQuery = "SELECT COUNT(*) AS CompletionRank FROM global_achievement_completions WHERE Namespace = ? AND Keyname = ? AND Timestamp < ?;";
        final String localQuery = "SELECT COUNT(*) AS CompletionRank FROM local_achievement_completions WHERE Namespace = ? AND Keyname = ? AND Timestamp < ?;";
        try (PreparedStatement preparedStatement = database.getConnection().getDatabaseConnection(targetDatabase).prepareStatement(targetDatabase == TargetDatabase.GLOBAL ? globalQuery : localQuery)) {
            for(Map.Entry<NamespacedKey, AchievementCompletion> completionEntry : completions.entrySet()) {
                final NamespacedKey namespacedKey = completionEntry.getKey();
                final AchievementCompletion achievementCompletion = completionEntry.getValue();
                preparedStatement.setString(1, namespacedKey.getNamespace());
                preparedStatement.setString(2, namespacedKey.getKey());
                preparedStatement.setTimestamp(3, achievementCompletion.getTimestamp());
                try(ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.first()) {
                        achievementCompletion.setCompletedRank(resultSet.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error loading completion ranks for {} {}: ", container.getClass().getSimpleName(), container.getUniqueId(), e).submit();
        }
    }

    /**
     * Loads all of the achivement completions from the db
     * @return
     */
    public CompletableFuture<ConcurrentHashMap<NamespacedKey, Integer>> loadTotalAchievementCompletions() {
        return CompletableFuture.supplyAsync(() -> {
            final ConcurrentHashMap<NamespacedKey, Integer> completions = new ConcurrentHashMap<>();
            final String globalQuery = "SELECT Namespace, Keyname, COUNT(DISTINCT User) AS CompletionRank FROM global_achievement_completions GROUP BY Namespace, Keyname;";
            final String localQuery = "SELECT Namespace, Keyname, COUNT(DISTINCT User) AS CompletionRank FROM local_achievement_completions GROUP BY Namespace, Keyname;";
            final Statement globalStatement =  new Statement(globalQuery);
            final Statement localStatement = new Statement(localQuery);
            try (CachedRowSet globalResults = database.executeQuery(globalStatement, TargetDatabase.GLOBAL).join()) {
                while (globalResults.next()) {
                    final String namespace = globalResults.getString(1);
                    final String keyname = globalResults.getString(2);
                    final int totalCompletions = globalResults.getInt(3);
                    final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                    completions.put(namespacedKey, totalCompletions);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            try (CachedRowSet localResults = database.executeQuery(localStatement, TargetDatabase.LOCAL).join()) {
                while (localResults.next()) {
                    final String namespace = localResults.getString(1);
                    final String keyname = localResults.getString(2);
                    final int totalCompletions = localResults.getInt(3);
                    final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                    completions.put(namespacedKey, totalCompletions);
                }
            } catch (SQLException e) {
                log.error("Error loading total achievement completions ",e).submit();
            }
            return completions;
        });
    }

    private static @NotNull Statement getStatement(@NotNull PropertyContainer container, TargetDatabase targetDatabase) {
        final String globalQuery = "SELECT Id, User, Namespace, Keyname, Timestamp FROM global_achievement_completions WHERE User = ?;";
        final String localQuery = "SELECT Id, User, Namespace, Keyname, Timestamp FROM local_achievement_completions WHERE User = ?;";
        return new Statement(targetDatabase == TargetDatabase.GLOBAL ? globalQuery : localQuery,
                new UuidStatementValue(container.getUniqueId())
                );
    }

    @NotNull
    public CompletableFuture<AchievementCompletion> saveCompletion(@NotNull PropertyContainer container, @NotNull NamespacedKey achievement) {
        final AchievementCompletion completion = new AchievementCompletion(UUID.randomUUID(),
                container.getUniqueId(),
                achievement,
                Timestamp.from(Instant.now())
        );
        return save(container, completion)
                .thenApply((obj) -> completion);
    }
}
