package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jooq.AggregateFunction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletions.ACHIEVEMENT_COMPLETIONS;
import static org.jooq.impl.DSL.countDistinct;

@Singleton
@CustomLog
public class AchievementCompletionRepository {

    private final Database database;
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    @Inject
    public AchievementCompletionRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Void> save(AchievementCompletion object) {
            return database.getAsyncDslContext().executeAsyncVoid(context -> {
                context.insertInto(ACHIEVEMENT_COMPLETIONS)
                        .set(ACHIEVEMENT_COMPLETIONS.ID, object.getId())
                        .set(ACHIEVEMENT_COMPLETIONS.CLIENT, object.getClient().getId())
                        .set(ACHIEVEMENT_COMPLETIONS.PERIOD, object.getPeriod())
                        .set(ACHIEVEMENT_COMPLETIONS.NAMESPACE, object.getKey().getNamespace())
                        .set(ACHIEVEMENT_COMPLETIONS.KEYNAME, object.getKey().getKey())
                        .set(ACHIEVEMENT_COMPLETIONS.TIMESTAMP, object.getTimestamp())
                        .execute();
            });
    }

    @NotNull
    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadForContainer(final @NotNull StatContainer container) {
        return database.getAsyncDslContext().executeAsync(context -> {
            final AchievementCompletionsConcurrentHashMap completions = new AchievementCompletionsConcurrentHashMap();
            context.select(ACHIEVEMENT_COMPLETIONS.ID, ACHIEVEMENT_COMPLETIONS.CLIENT, ACHIEVEMENT_COMPLETIONS.PERIOD, ACHIEVEMENT_COMPLETIONS.NAMESPACE, ACHIEVEMENT_COMPLETIONS.KEYNAME, ACHIEVEMENT_COMPLETIONS.TIMESTAMP)
                    .from(ACHIEVEMENT_COMPLETIONS)
                    .where(ACHIEVEMENT_COMPLETIONS.CLIENT.eq(container.getClient().getId()))
                    .fetch()
                    .forEach(record -> {
                                final long completionId = record.get(ACHIEVEMENT_COMPLETIONS.ID);
                                final String period = record.get(ACHIEVEMENT_COMPLETIONS.PERIOD);
                                final String namespace = record.get(ACHIEVEMENT_COMPLETIONS.NAMESPACE);
                                final String key = record.get(ACHIEVEMENT_COMPLETIONS.KEYNAME);
                                final LocalDateTime timestamp = record.get(ACHIEVEMENT_COMPLETIONS.TIMESTAMP);
                                final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                        container.getClient(),
                                        new NamespacedKey(namespace, key),
                                        period,
                                        timestamp
                                );
                                completions.addCompletion(achievementCompletion);
                            }
                    );
            return completions;
        }).thenCompose(achievementCompletions -> loadCompletionRanks(achievementCompletions));
    }

    /**
     * Loads the {@link AchievementCompletion#getCompletedRank()} for each achievement
     * @param container the {@link PropertyContainer}
     * @param completions the {@link ConcurrentHashMap} to be modified in place
     */
    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadCompletionRanks(AchievementCompletionsConcurrentHashMap completions) {

        return database.getAsyncDslContext().executeAsync(context -> {
            for (AchievementCompletion completion : completions) {
                final NamespacedKey namespacedKey = completion.getKey();
                int rank = context.fetchCount(ACHIEVEMENT_COMPLETIONS,
                                ACHIEVEMENT_COMPLETIONS.PERIOD.eq(completion.getPeriod()),
                        ACHIEVEMENT_COMPLETIONS.NAMESPACE.eq(namespacedKey.getNamespace()),
                        ACHIEVEMENT_COMPLETIONS.KEYNAME.eq(namespacedKey.getKey()),
                        ACHIEVEMENT_COMPLETIONS.TIMESTAMP.lt(completion.getTimestamp())
                        );
                completion.setCompletedRank(rank);
            }
            return completions;
        });
    }

    /**
     * Loads all of the achivement completions from the db
     * @return
     */
    public CompletableFuture<ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, Integer>>> loadTotalAchievementCompletions() {

        return database.getAsyncDslContext().executeAsync(context -> {
            final ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, Integer>> completions = new ConcurrentHashMap<>();
            final AggregateFunction<Integer> totalCompletions = countDistinct(ACHIEVEMENT_COMPLETIONS.CLIENT);
            context.select(ACHIEVEMENT_COMPLETIONS.PERIOD, ACHIEVEMENT_COMPLETIONS.NAMESPACE, ACHIEVEMENT_COMPLETIONS.KEYNAME, totalCompletions)
                    .from(ACHIEVEMENT_COMPLETIONS)
                    .groupBy(
                            ACHIEVEMENT_COMPLETIONS.PERIOD,
                            ACHIEVEMENT_COMPLETIONS.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS.KEYNAME
                    ).fetch()
                    .forEach(record -> {
                        final String period = record.get(ACHIEVEMENT_COMPLETIONS.PERIOD);
                        final String namespace = record.get(ACHIEVEMENT_COMPLETIONS.NAMESPACE);
                        final String keyname = record.get(ACHIEVEMENT_COMPLETIONS.KEYNAME);
                        final int numCompletions = record.get(totalCompletions);
                        final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                        completions.computeIfAbsent(period, (k) ->
                            new ConcurrentHashMap<>()
                        ).put(namespacedKey, numCompletions);
                    }
                    );
            return completions;
        });
    }


    @NotNull
    public CompletableFuture<AchievementCompletion> saveCompletion(@NotNull StatContainer container, @NotNull NamespacedKey achievement, String period) {
        final AchievementCompletion completion = new AchievementCompletion(ID_GENERATOR.nextId(),
                container.getClient(),
                achievement,
                period,
                //in testing, db was not saving the timestamp with the same precision
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );
        return save(completion)
                .thenApply((obj) -> {
                    loadCompletionRanks(new AchievementCompletionsConcurrentHashMap().addCompletion(completion));
                    return completion;
                        }
                );
    }
}
