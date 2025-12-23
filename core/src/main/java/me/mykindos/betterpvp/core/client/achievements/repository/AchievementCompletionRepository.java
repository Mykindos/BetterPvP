package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jooq.AggregateFunction;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletionsAll.ACHIEVEMENT_COMPLETIONS_ALL;
import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletionsRealm.ACHIEVEMENT_COMPLETIONS_REALM;
import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletionsSeason.ACHIEVEMENT_COMPLETIONS_SEASON;
import static org.jooq.impl.DSL.countDistinct;

@Singleton
@CustomLog
public class AchievementCompletionRepository {

    private final Database database;
    private final RealmManager realmManager;
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    @Inject
    public AchievementCompletionRepository(Database database, RealmManager realmManager) {
        this.database = database;
        this.realmManager = realmManager;
    }

    public CompletableFuture<Void> save(AchievementCompletion object) {
        return switch (object.getAchievementFilterType()) {
            case ALL ->
                saveAll(object);
            case SEASON ->
                saveSeason(object);
            case REALM ->
                saveRealm(object);
        };
    }

    private CompletableFuture<Void> saveAll(AchievementCompletion object) {
            return database.getAsyncDslContext().executeAsyncVoid(context -> {
                context.insertInto(ACHIEVEMENT_COMPLETIONS_ALL)
                        .set(ACHIEVEMENT_COMPLETIONS_ALL.ID, object.getId())
                        .set(ACHIEVEMENT_COMPLETIONS_ALL.CLIENT, object.getClient().getId())
                        .set(ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE, object.getKey().getNamespace())
                        .set(ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME, object.getKey().getKey())
                        .set(ACHIEVEMENT_COMPLETIONS_ALL.TIMESTAMP, object.getTimestamp())
                        .execute();
            });
    }
    private CompletableFuture<Void> saveSeason(AchievementCompletion object) {
            return database.getAsyncDslContext().executeAsyncVoid(context -> {
                final Season season = ((Season) Objects.requireNonNull(object.getPeriod()));
                context.insertInto(ACHIEVEMENT_COMPLETIONS_SEASON)
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.ID, object.getId())
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.CLIENT, object.getClient().getId())
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.SEASON, season.getId())
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE, object.getKey().getNamespace())
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME, object.getKey().getKey())
                        .set(ACHIEVEMENT_COMPLETIONS_SEASON.TIMESTAMP, object.getTimestamp())
                        .execute();
            });
    }
    private CompletableFuture<Void> saveRealm(AchievementCompletion object) {
            return database.getAsyncDslContext().executeAsyncVoid(context -> {
                final Realm realm = ((Realm) Objects.requireNonNull(object.getPeriod()));
                context.insertInto(ACHIEVEMENT_COMPLETIONS_REALM)
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.ID, object.getId())
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.CLIENT, object.getClient().getId())
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.REALM, realm.getId())
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE, object.getKey().getNamespace())
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME, object.getKey().getKey())
                        .set(ACHIEVEMENT_COMPLETIONS_REALM.TIMESTAMP, object.getTimestamp())
                        .execute();
            });
    }

    @NotNull
    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadForContainer(final @NotNull StatContainer container) {
        final AchievementCompletionsConcurrentHashMap completions = new AchievementCompletionsConcurrentHashMap();
        return CompletableFuture.allOf(
                loadAll(container, completions),
                    loadSeason(container, completions),
                    loadRealm(container, completions)
                ).thenCompose(v -> loadCompletionRanks(container, completions));
    }

    private CompletableFuture<AchievementCompletionsConcurrentHashMap> loadAll(StatContainer container, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            context.select(ACHIEVEMENT_COMPLETIONS_ALL.ID,
                            ACHIEVEMENT_COMPLETIONS_ALL.CLIENT,
                            ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME,
                            ACHIEVEMENT_COMPLETIONS_ALL.TIMESTAMP)
                    .from(ACHIEVEMENT_COMPLETIONS_ALL)
                    .where(ACHIEVEMENT_COMPLETIONS_ALL.CLIENT.eq(container.getClient().getId()))
                    .fetch()
                    .forEach(rec -> {
                                final long completionId = rec.get(ACHIEVEMENT_COMPLETIONS_ALL.ID);
                                final String namespace = rec.get(ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE);
                                final String key = rec.get(ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME);
                                final LocalDateTime timestamp = rec.get(ACHIEVEMENT_COMPLETIONS_ALL.TIMESTAMP);
                                final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                        container.getClient(),
                                        new NamespacedKey(namespace, key),
                                        StatFilterType.ALL,
                                        null,
                                        timestamp
                                );
                                completions.addCompletion(achievementCompletion);
                            }
                    );
            return completions;
        });
    }

    private CompletableFuture<AchievementCompletionsConcurrentHashMap> loadSeason(StatContainer container, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            context.select(ACHIEVEMENT_COMPLETIONS_SEASON.ID,
                            ACHIEVEMENT_COMPLETIONS_SEASON.CLIENT,
                            ACHIEVEMENT_COMPLETIONS_SEASON.SEASON,
                            ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME,
                            ACHIEVEMENT_COMPLETIONS_SEASON.TIMESTAMP)
                    .from(ACHIEVEMENT_COMPLETIONS_SEASON)
                    .where(ACHIEVEMENT_COMPLETIONS_SEASON.CLIENT.eq(container.getClient().getId()))
                    .fetch()
                    .forEach(rec -> {
                                final long completionId = rec.get(ACHIEVEMENT_COMPLETIONS_SEASON.ID);
                                final String namespace = rec.get(ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE);
                                final int season = rec.get(ACHIEVEMENT_COMPLETIONS_SEASON.SEASON);
                                final String key = rec.get(ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME);
                                final LocalDateTime timestamp = rec.get(ACHIEVEMENT_COMPLETIONS_SEASON.TIMESTAMP);
                                final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                        container.getClient(),
                                        new NamespacedKey(namespace, key),
                                        StatFilterType.SEASON,
                                        realmManager.getSeason(season).orElseThrow(),
                                        timestamp
                                );
                                completions.addCompletion(achievementCompletion);
                            }
                    );
            return completions;
        });
    }
    private CompletableFuture<AchievementCompletionsConcurrentHashMap> loadRealm(StatContainer container, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            context.select(ACHIEVEMENT_COMPLETIONS_REALM.ID,
                            ACHIEVEMENT_COMPLETIONS_REALM.CLIENT,
                            ACHIEVEMENT_COMPLETIONS_REALM.REALM,
                            ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME,
                            ACHIEVEMENT_COMPLETIONS_REALM.TIMESTAMP)
                    .from(ACHIEVEMENT_COMPLETIONS_REALM)
                    .where(ACHIEVEMENT_COMPLETIONS_REALM.CLIENT.eq(container.getClient().getId()))
                    .fetch()
                    .forEach(rec -> {
                                final long completionId = rec.get(ACHIEVEMENT_COMPLETIONS_REALM.ID);
                                final String namespace = rec.get(ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE);
                                final int realm = rec.get(ACHIEVEMENT_COMPLETIONS_REALM.REALM);
                                final String key = rec.get(ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME);
                                final LocalDateTime timestamp = rec.get(ACHIEVEMENT_COMPLETIONS_REALM.TIMESTAMP);
                                final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                        container.getClient(),
                                        new NamespacedKey(namespace, key),
                                        StatFilterType.SEASON,
                                        realmManager.getObject(realm).orElseThrow(),
                                        timestamp
                                );
                                completions.addCompletion(achievementCompletion);
                            }
                    );
            return completions;
        });
    }

    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadCompletionRanks(StatContainer statContainer, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            Map<NamespacedKey, AchievementCompletion> completionMap = completions.asMap();
                    Table<Record3<String, String, LocalDateTime>> ALL =
                            DSL.select(
                                            ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE,
                                            ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME,
                                            ACHIEVEMENT_COMPLETIONS_ALL.TIMESTAMP
                                    )
                                    .from(ACHIEVEMENT_COMPLETIONS_ALL)
                                    .where(ACHIEVEMENT_COMPLETIONS_ALL.CLIENT.eq(statContainer.getClient().getId()))

                                    .unionAll(
                                            DSL.select(
                                                            ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE,
                                                            ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME,
                                                            ACHIEVEMENT_COMPLETIONS_SEASON.TIMESTAMP
                                                    )
                                                    .from(ACHIEVEMENT_COMPLETIONS_SEASON)
                                                    .where(ACHIEVEMENT_COMPLETIONS_SEASON.CLIENT.eq(statContainer.getClient().getId()))
                                    )

                                    .unionAll(
                                            DSL.select(
                                                            ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE,
                                                            ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME,
                                                            ACHIEVEMENT_COMPLETIONS_REALM.TIMESTAMP
                                                    )
                                                    .from(ACHIEVEMENT_COMPLETIONS_REALM)
                                                    .where(ACHIEVEMENT_COMPLETIONS_REALM.CLIENT.eq(statContainer.getClient().getId()))
                                    )
                                    .asTable("all_completions");
                    Field<Integer> rankField =
                            DSL.rowNumber()
                                    .over(
                                            DSL.partitionBy(
                                                            ALL.field("Namespace"),
                                                            ALL.field("Keyname")
                                                    )
                                                    .orderBy(ALL.field("Timestamp"))
                                    )
                                    .minus(DSL.inline(1))
                                    .as("rank");
                   Field<String> namespaceField = ALL.field("Namespace", String.class);
                   Field<String> keynameField = ALL.field("Keyname", String.class);

                    Result<Record3<String, String, Integer>> result =
                            context.select(
                                            namespaceField,
                                            keynameField,
                                            rankField
                                    )
                                    .from(ALL)
                                    .fetch();
                    result.forEach(rec -> {
                        final String namespace = rec.get(namespaceField);
                        final String keyName = rec.get(keynameField);
                        final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyName);
                        final int rank = rec.get(rankField);
                        completionMap.get(namespacedKey).setCompletedRank(rank);
                    });
                    return completions;
        });
    }

    public CompletableFuture<ConcurrentMap<NamespacedKey, Integer>> loadTotalAllAchievementCompletions() {
        return database.getAsyncDslContext().executeAsync(context -> {
            final ConcurrentHashMap<NamespacedKey, Integer> completions = new ConcurrentHashMap<>();
            final AggregateFunction<Integer> totalCompletions = countDistinct(ACHIEVEMENT_COMPLETIONS_ALL.CLIENT);
            context.select(ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME,
                            totalCompletions)
                    .from(ACHIEVEMENT_COMPLETIONS_ALL)
                    .groupBy(
                            ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME
                    ).fetch()
                    .forEach(record -> {
                        final String namespace = record.get(ACHIEVEMENT_COMPLETIONS_ALL.NAMESPACE);
                        final String keyname = record.get(ACHIEVEMENT_COMPLETIONS_ALL.KEYNAME);
                        final int numCompletions = record.get(totalCompletions);
                        final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                        completions.put(namespacedKey, numCompletions);
                    });
            return completions;
        });
    }

    public CompletableFuture<ConcurrentMap<Season, ConcurrentMap<NamespacedKey, Integer>>> loadTotalSeasonAchievementCompletions() {
        return database.getAsyncDslContext().executeAsync(context -> {
            final ConcurrentMap<Season, ConcurrentMap<NamespacedKey, Integer>> completions = new ConcurrentHashMap<>();
            final AggregateFunction<Integer> totalCompletions = countDistinct(ACHIEVEMENT_COMPLETIONS_SEASON.CLIENT);
            context.select(ACHIEVEMENT_COMPLETIONS_SEASON.SEASON,
                            ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME,
                            totalCompletions)
                    .from(ACHIEVEMENT_COMPLETIONS_SEASON)
                    .groupBy(
                            ACHIEVEMENT_COMPLETIONS_SEASON.SEASON,
                            ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME
                    ).fetch()
                    .forEach(record -> {
                        final int season = record.get(ACHIEVEMENT_COMPLETIONS_SEASON.SEASON);
                        final String namespace = record.get(ACHIEVEMENT_COMPLETIONS_SEASON.NAMESPACE);
                        final String keyname = record.get(ACHIEVEMENT_COMPLETIONS_SEASON.KEYNAME);
                        final int numCompletions = record.get(totalCompletions);
                        final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                        completions.computeIfAbsent(realmManager.getSeason(season).orElseThrow(), (k) ->
                            new ConcurrentHashMap<>()
                        ).put(namespacedKey, numCompletions);
                    });
            return completions;
        });
    }

    public CompletableFuture<ConcurrentMap<Realm, ConcurrentMap<NamespacedKey, Integer>>> loadTotalRealmAchievementCompletions() {
        return database.getAsyncDslContext().executeAsync(context -> {
            final ConcurrentMap<Realm, ConcurrentMap<NamespacedKey, Integer>> completions = new ConcurrentHashMap<>();
            final AggregateFunction<Integer> totalCompletions = countDistinct(ACHIEVEMENT_COMPLETIONS_REALM.CLIENT);
            context.select(ACHIEVEMENT_COMPLETIONS_REALM.REALM,
                            ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME,
                            totalCompletions)
                    .from(ACHIEVEMENT_COMPLETIONS_REALM)
                    .groupBy(
                            ACHIEVEMENT_COMPLETIONS_REALM.REALM,
                            ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE,
                            ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME
                    ).fetch()
                    .forEach(record -> {
                        final int realm = record.get(ACHIEVEMENT_COMPLETIONS_REALM.REALM);
                        final String namespace = record.get(ACHIEVEMENT_COMPLETIONS_REALM.NAMESPACE);
                        final String keyname = record.get(ACHIEVEMENT_COMPLETIONS_REALM.KEYNAME);
                        final int numCompletions = record.get(totalCompletions);
                        final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyname);
                        completions.computeIfAbsent(realmManager.getObject(realm).orElseThrow(), (k) ->
                                new ConcurrentHashMap<>()
                        ).put(namespacedKey, numCompletions);
                    });
            return completions;
        });
    }


    @NotNull
    public CompletableFuture<AchievementCompletion> saveCompletion(@NotNull StatContainer container, @NotNull IAchievement achievement, Period period) {
        final AchievementCompletion completion = new AchievementCompletion(ID_GENERATOR.nextId(),
                container.getClient(),
                achievement.getNamespacedKey(),
                achievement.getAchievementFilterType(),
                period,
                //in testing, db was not saving the timestamp with the same precision
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );
        return save(completion)
                .thenApply((obj) -> {
                    loadCompletionRanks(container, new AchievementCompletionsConcurrentHashMap().addCompletion(completion));
                    return completion;
                });
    }
}
