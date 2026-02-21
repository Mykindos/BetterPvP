package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.Tables;
import me.mykindos.betterpvp.core.database.jooq.tables.records.AchievementCompletionsRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetAchievementCompletionsRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetClientAchievementRanksRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetTotalAchievementCompletionsRecord;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletions.ACHIEVEMENT_COMPLETIONS;
import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletionsRealm.ACHIEVEMENT_COMPLETIONS_REALM;
import static me.mykindos.betterpvp.core.database.jooq.tables.AchievementCompletionsSeason.ACHIEVEMENT_COMPLETIONS_SEASON;

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
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.transaction(config -> {
                DSLContext ctxl = DSL.using(config);
                AchievementCompletionsRecord record = ctxl.insertInto(ACHIEVEMENT_COMPLETIONS)
                        .set(ACHIEVEMENT_COMPLETIONS.ID, object.getId())
                        .set(ACHIEVEMENT_COMPLETIONS.CLIENT, object.getClient().getId())
                        .set(ACHIEVEMENT_COMPLETIONS.NAMESPACE, object.getKey().getNamespace())
                        .set(ACHIEVEMENT_COMPLETIONS.KEYNAME, object.getKey().getKey())
                        .set(ACHIEVEMENT_COMPLETIONS.TIMESTAMP, object.getTimestamp())
                        .returning(ACHIEVEMENT_COMPLETIONS.ID)
                        .fetchOne();
                final Period period = object.getPeriod();
                if (period == null || record == null) return;
                long id = record.getId();
                if (period instanceof Season season) {
                    ctxl.insertInto(ACHIEVEMENT_COMPLETIONS_SEASON)
                            .set(ACHIEVEMENT_COMPLETIONS_SEASON.ID, id)
                            .set(ACHIEVEMENT_COMPLETIONS_SEASON.SEASON, season.getId())
                            .execute();
                    return;
                }
                if (period instanceof Realm realm) {
                    ctxl.insertInto(ACHIEVEMENT_COMPLETIONS_REALM)
                            .set(ACHIEVEMENT_COMPLETIONS_REALM.ID, id)
                            .set(ACHIEVEMENT_COMPLETIONS_REALM.REALM, realm.getId())
                            .execute();
                    return;
                }
            });
        });
    }

    @NotNull
    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadForContainer(final @NotNull StatContainer container) {
        final AchievementCompletionsConcurrentHashMap completions = new AchievementCompletionsConcurrentHashMap();
        return loadCompletions(container, completions).exceptionally(throwable -> {
            log.error("Error loading achievement completions for client {}", container.getClient().getName(), throwable).submit();
            return completions;})
                .thenCompose(v -> loadCompletionRanks(container, completions)).exceptionally(throwable -> {
                            log.error("Error loading achievement completion ranks for client {}", container.getClient().getName(), throwable).submit();
                            return completions;
                        }
                );
    }

    @Nullable
    private Period getPeriod(GetAchievementCompletionsRecord completion) {
        if (completion.getRealm() != null) {
            return realmManager.getObject(completion.getRealm()).orElseThrow();
        }
        if (completion.getSeason() != null) {
            return realmManager.getSeason(completion.getSeason()).orElseThrow();
        }
        return null;
    }

    private CompletableFuture<AchievementCompletionsConcurrentHashMap> loadCompletions(StatContainer container, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            try {
                Tables.GET_ACHIEVEMENT_COMPLETIONS(context.configuration(), container.getClient().getId())
                        .forEach(completion -> {
                            final long completionId = completion.getId();
                            final String namespace = completion.getNamespace();
                            final String key = completion.getKeyname();
                            final LocalDateTime timestamp = completion.getTimeachieved();
                            final Period period = getPeriod(completion);
                            final AchievementCompletion achievementCompletion = new AchievementCompletion(completionId,
                                    container.getClient(),
                                    new NamespacedKey(namespace, key),
                                    StatFilterType.ALL,
                                    period,
                                    timestamp
                            );
                            completions.addCompletion(achievementCompletion);
                        });
                return completions;
            } catch (Exception e) {
                log.error("Error loading achievement completions for client {}", container.getClient().getName(), e).submit();
                return completions;
            }
        });
    }

    @Nullable
    private Period getPeriod(GetClientAchievementRanksRecord completion) {
        if (completion.getRealm() != null) {
            return realmManager.getObject(completion.getRealm()).orElseThrow();
        }
        if (completion.getSeason() != null) {
            return realmManager.getSeason(completion.getSeason()).orElseThrow();
        }
        return null;
    }

    public CompletableFuture<AchievementCompletionsConcurrentHashMap> loadCompletionRanks(StatContainer statContainer, AchievementCompletionsConcurrentHashMap completions) {
        return database.getAsyncDslContext().executeAsync(context -> {
            try {
                Tables.GET_CLIENT_ACHIEVEMENT_RANKS(context.configuration(), statContainer.getClient().getId())
                        .forEach(achievementRank -> {
                            final String namespace = achievementRank.getNamespace();
                            final String keyName = achievementRank.getKeyname();
                            final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyName);
                            final Period period = getPeriod(achievementRank);
                            final int rank = Math.toIntExact(achievementRank.getRank());
                            completions.getCompletion(namespacedKey, StatFilterType.fromPeriod(period), period).orElseThrow().setCompletedRank(rank);
                        });
                return completions;
            } catch (Exception e) {
                log.error("Error loading achievement completion ranks for client {}", statContainer.getClient().getName(), e).submit();
                return completions;
            }
        });
    }

    @Nullable
    private Period getPeriod(GetTotalAchievementCompletionsRecord completion) {
        if (completion.getRealm() != null) {
            return realmManager.getObject(completion.getRealm()).orElseThrow();
        }
        if (completion.getSeason() != null) {
            return realmManager.getSeason(completion.getSeason()).orElseThrow();
        }
        return null;
    }

    public CompletableFuture<Void> updateTotalCompletions(ConcurrentMap<NamespacedKey, Integer> allMap, ConcurrentMap<Season, ConcurrentMap<NamespacedKey, Integer>> seasonMap, ConcurrentMap<Realm, ConcurrentMap<NamespacedKey, Integer>> realmMap) {
        return database.getAsyncDslContext().executeAsync(context -> {
            try {
                Tables.GET_TOTAL_ACHIEVEMENT_COMPLETIONS(context.configuration())
                        .forEach(achievementTotal -> {
                            final String namespace = achievementTotal.getNamespace();
                            final String keyName = achievementTotal.getKeyname();
                            final NamespacedKey namespacedKey = new NamespacedKey(namespace, keyName);
                            final Period period = getPeriod(achievementTotal);
                            final int total = Math.toIntExact(achievementTotal.getTotal());
                            switch (period) {
                                case null -> allMap.put(namespacedKey, total);
                                case Season season ->
                                        seasonMap.computeIfAbsent(season, (k) -> new ConcurrentHashMap<>()).put(namespacedKey, total);
                                case Realm realm ->
                                        realmMap.computeIfAbsent(realm, (k) -> new ConcurrentHashMap<>()).put(namespacedKey, total);
                                default -> {
                                }
                            }
                        });
                return null;
            } catch (Exception e) {
                log.error("Error loading total achievement completions", e).submit();
                return null;
            }
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
