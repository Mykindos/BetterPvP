package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
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

    public void save(PropertyContainer container, AchievementCompletion object) {

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

        database.executeUpdate(updateStatement, targetDatabase);

    }

    @NotNull
    public ConcurrentHashMap<NamespacedKey, AchievementCompletion> loadForContainer(@NotNull PropertyContainer container) {
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
            log.error("Error getting AchievementCompletions for {}: ", container.getUniqueId(), e).submit();
        }

        return completions;
    }

    private static @NotNull Statement getStatement(@NotNull PropertyContainer container, TargetDatabase targetDatabase) {
        final String globalQuery = "SELECT Id, User, Namespace, Keyname, Timestamp FROM global_achievement_completions WHERE User = ?;";
        final String localQuery = "SELECT Id, User, Namespace, Keyname, Timestamp FROM local_achievement_completions WHERE User = ?;";
        return new Statement(targetDatabase == TargetDatabase.GLOBAL ? globalQuery : localQuery,
                new UuidStatementValue(container.getUniqueId())
                );
    }

    @NotNull
    public AchievementCompletion saveCompletion(@NotNull PropertyContainer container, @NotNull NamespacedKey achievement) {
        final AchievementCompletion completion = new AchievementCompletion(UUID.randomUUID(),
                container.getUniqueId(),
                achievement,
                Timestamp.from(Instant.now())
        );
        save(container, completion);
        return completion;
    }
}
