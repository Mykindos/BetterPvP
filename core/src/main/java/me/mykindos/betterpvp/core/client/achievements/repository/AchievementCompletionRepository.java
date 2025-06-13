package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.rowset.CachedRowSet;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
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

    public CompletableFuture<Void> save(StatContainer container, AchievementCompletion object) {
            final Statement updateStatement = Statement.builder()
                    .insertInto("achievement_completions", "Id", "User", "Period", "Namespace", "Keyname", "Timestamp")
                    .values(new UuidStatementValue(object.getId()),
                            new UuidStatementValue(object.getUser()),
                            new StringStatementValue(object.getPeriod()),
                            new StringStatementValue(object.getKey().getNamespace()),
                            new StringStatementValue(object.getKey().getKey()),
                            new TimestampStatementValue(object.getTimestamp()))
                    .build();

            return database.executeUpdate(updateStatement, TargetDatabase.GLOBAL);
    }

    @NotNull
    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadForContainer(@NotNull StatContainer container) {
        return CompletableFuture.supplyAsync(() -> {
                    final AchievementCompletionsConcurrentHashMap completions = new AchievementCompletionsConcurrentHashMap();

            final Statement queryStatement = Statement.builder()
                    .select("achievement_completions", "Id", "User", "Period", "Namespace", "Keyname", "Timestamp")
                    .where("User", "=", new UuidStatementValue(container.getUniqueId()))
                    .build();

                    try (final CachedRowSet results = database.executeQuery(queryStatement, TargetDatabase.GLOBAL).join()) {
                        while (results.next()) {
                            final UUID completionId = UUID.fromString(results.getString("Id"));
                            final UUID user = UUID.fromString(results.getString("User"));
                            final String period = results.getString("Period");
                            final String namespace = results.getString("Namespace");
                            final String key = results.getString("Keyname");
                            final Timestamp timestamp = results.getTimestamp("Timestamp");

                            final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                    user,
                                    new NamespacedKey(namespace, key),
                                    period,
                                    timestamp
                            );

                            completions.addCompletion(achievementCompletion);
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
    public AchievementCompletionsConcurrentHashMap loadCompletionRanks(StatContainer container, AchievementCompletionsConcurrentHashMap completions) {
        final String query= "SELECT COUNT(*) AS CompletionRank FROM achievement_completions WHERE Period = ? AND Namespace = ? AND Keyname = ? AND Timestamp < ?;";
        try (PreparedStatement preparedStatement = database.getConnection()
                .getDatabaseConnection(TargetDatabase.GLOBAL)
                .prepareStatement(query)
        ) {
            for(AchievementCompletion completion : completions) {
                final NamespacedKey namespacedKey = completion.getKey();
                preparedStatement.setString(1, completion.getPeriod());
                preparedStatement.setString(2, namespacedKey.getNamespace());
                preparedStatement.setString(3, namespacedKey.getKey());
                preparedStatement.setTimestamp(4, completion.getTimestamp());
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        completion.setCompletedRank(resultSet.getInt(1) + 1);
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error loading completion ranks for {} {}: ", container.getClass().getSimpleName(), container.getUniqueId(), e).submit();
        }
        return completions;
    }

    /**
     * Loads all of the achivement completions from the db
     * @return
     */
    public CompletableFuture<ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, Integer>>> loadTotalAchievementCompletions() {
        return CompletableFuture.supplyAsync(() -> {
            final ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, Integer>> completions = new ConcurrentHashMap<>();
            final Statement statement = Statement.builder()
                    .select("achievement_completions", "Period", "Namespace", "Keyname", "COUNT(DISTINCT User) AS TotalCompletions")
                    .groupBy("Period")
                    .groupBy("Namespace")
                    .groupBy("Keyname")
                    .build();
            try (CachedRowSet results = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
                while (results.next()) {
                    final String period = results.getString("Period");
                    final String namespace = results.getString("Namespace");
                    final String keyname = results.getString("Keyname");
                    final int totalCompletions = results.getInt("TotalCompletions");
                    final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                    completions.compute(period, (k, v) -> {
                        if (v == null) {
                            v = new ConcurrentHashMap<>();
                        }
                        v.put(namespacedKey, totalCompletions);
                        return v;
                    });
                }
            } catch (SQLException e) {
                log.error("Error fetching total completions ", e).submit();
            }
            return completions;
        });
    }


    @NotNull
    public CompletableFuture<AchievementCompletion> saveCompletion(@NotNull StatContainer container, @NotNull NamespacedKey achievement, String period) {
        final AchievementCompletion completion = new AchievementCompletion(UUID.randomUUID(),
                container.getUniqueId(),
                achievement,
                period,
                //in testing, db was not saving the timestamp with the same precision
                Timestamp.from(Instant.now().truncatedTo(ChronoUnit.SECONDS))
        );
        return save(container, completion)
                .thenApply((obj) -> {
                    loadCompletionRanks(container,
                            new AchievementCompletionsConcurrentHashMap().addCompletion(completion));
                            return completion;
                        }
                );
    }
}
