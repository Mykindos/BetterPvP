package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@Singleton
@CustomLog
public class AchievementManager extends Manager<NamespacedKey, IAchievement> implements Reloadable {
    @Getter
    private final AchievementCategoryManager achievementCategoryManager;
    private final ClientManager clientManager;
    //todo, update this across server instances
    //Period, Achievement, TotalCount
    private final ConcurrentMap<NamespacedKey, Integer> totalAllAchievementCompletions;
    private final ConcurrentMap<Season, ConcurrentMap<NamespacedKey, Integer>> totalSeasonCompletions;
    private final ConcurrentMap<Realm, ConcurrentMap<NamespacedKey, Integer>> totalRealmCompletions;
    private final AchievementCompletionRepository achievementCompletionRepository;
    /**
     * Index: watched-stat key -> achievements that care about that stat.
     * Built incrementally as achievements are registered via {@link #addObject(NamespacedKey, IAchievement, BPvPPlugin)}.
     */
    private final ConcurrentMap<IStat, Set<IAchievement>> statIndex = new ConcurrentHashMap<>();
    /**
     * Maps each achievement key to the plugin that registered it, so we can reload configs correctly.
     */
    private final ConcurrentMap<NamespacedKey, BPvPPlugin> achievementPlugins = new ConcurrentHashMap<>();
    @Inject
    public AchievementManager(AchievementCategoryManager achievementCategoryManager,
                              AchievementCompletionRepository achievementCompletionRepository,
                              Core core,
                              ClientManager clientManager) {
        this.achievementCategoryManager = achievementCategoryManager;
        this.achievementCompletionRepository = achievementCompletionRepository;
        this.clientManager = clientManager;
        totalAllAchievementCompletions = new ConcurrentHashMap<>();
        totalSeasonCompletions = new ConcurrentHashMap<>();
        totalRealmCompletions = new ConcurrentHashMap<>();
        updateTotalAchievementCompletions();
    }
    /**
     * Register an achievement and associate it with its owning plugin.
     * Builds the stat-to-achievement index from the achievement watched stats.
     */
    public void addObject(NamespacedKey key, IAchievement achievement, BPvPPlugin plugin) {
        super.addObject(key, achievement);
        achievementPlugins.put(key, plugin);
        for (IStat watchedStat : achievement.getWatchedStats()) {
            statIndex.computeIfAbsent(watchedStat, k -> Collections.synchronizedSet(new HashSet<>()))
                    .add(achievement);
        }
    }
    /**
     * Returns all achievements that have a watched stat which contains the given stat.
     * Used by the centralised event handler to avoid iterating every achievement on every stat change.
     */
    public Set<IAchievement> getAchievementsForStat(IStat eventStat) {
        Set<IAchievement> result = new HashSet<>();
        for (Map.Entry<IStat, Set<IAchievement>> entry : statIndex.entrySet()) {
            if (entry.getKey().containsStat(eventStat)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }
    /**
     * Re-reads each achievement config section from its owning plugin achievements config.
     * Called automatically when the plugin is reloaded.
     */
    @Override
    public void reload() {
        getObjects().forEach((key, achievement) -> {
            BPvPPlugin plugin = achievementPlugins.get(key);
            if (plugin == null) {
                log.warn("No plugin registered for achievement {}, skipping config reload", key.asString()).submit();
                return;
            }
            try {
                achievement.loadConfig(plugin.getConfig("achievements"));
            } catch (Exception e) {
                log.error("Failed to reload config for achievement {}", key.asString(), e).submit();
            }
        });
        log.info("Reloaded {} achievement configs", getObjects().size()).submit();
    }
    public CompletableFuture<Void> loadAchievementCompletionsAsync(Client client) {
        return achievementCompletionRepository.loadForContainer(client.getStatContainer())
                .exceptionally(throwable -> {
                    log.error("Failed to load achievement completions for {}", client.getName(), throwable).submit();
                    return null;
                })
                .thenAccept(completions -> {
                    try {
                        completions.forEach(completion -> {
                            int total = getTotalCompletion(getObject(completion.getKey()).orElseThrow(), completion.getPeriod());
                            completion.setTotalCompletions(total);
                        });
                        client.getStatContainer().getAchievementCompletions().fromOther(completions);
                    } catch (Exception e) {
                        log.error("Failed to process loaded achievement completions for {}", client.getName(), e).submit();
                    }
                });
    }
    public CompletableFuture<Void> saveCompletion(StatContainer container, IAchievement achievement, Period period) {
        log.info("Start save completion for {} {}", container.getClient().getName(), achievement.getName()).submit();
        return achievementCompletionRepository.saveCompletion(container, achievement, period)
                .thenAccept(achievementCompletion -> {
                    container.getAchievementCompletions().addCompletion(achievementCompletion);
                    updateTotalCompletions(achievement, achievementCompletion.getPeriod());
                    log.info("End save completion for {} {}", container.getClient().getName(), achievement.getName()).submit();
                });
    }
    public int getTotalCompletion(IAchievement achievement, Period period) {
        return switch (achievement.getAchievementFilterType()) {
            case ALL -> totalAllAchievementCompletions.get(achievement.getNamespacedKey());
            case SEASON -> totalSeasonCompletions.get((Season) period).get(achievement.getNamespacedKey());
            case REALM -> totalRealmCompletions.get((Realm) period).get(achievement.getNamespacedKey());
        };
    }
    public void updateTotalCompletions(IAchievement achievement, Period period) {
        int total = 0;
        switch (achievement.getAchievementFilterType()) {
            case ALL ->
                total = totalAllAchievementCompletions.compute(achievement.getNamespacedKey(),
                        (key, value) -> value == null ? 1 : value + 1);
            case SEASON ->
                total = totalSeasonCompletions.computeIfAbsent((Season) period, k -> new ConcurrentHashMap<>())
                        .compute(achievement.getNamespacedKey(), (key, value) -> value == null ? 1 : value + 1);
            case REALM ->
                total = totalRealmCompletions.computeIfAbsent((Realm) period, k -> new ConcurrentHashMap<>())
                        .compute(achievement.getNamespacedKey(), (key, value) -> value == null ? 1 : value + 1);
        }
        final int finalTotal = total;
        clientManager.getLoaded().forEach(client ->
                client.getStatContainer().getAchievementCompletions()
                        .getCompletion(achievement, period)
                        .ifPresent(c -> c.setTotalCompletions(finalTotal)));
    }
    public CompletableFuture<Void> updateTotalAchievementCompletions() {
        return achievementCompletionRepository.updateTotalCompletions(
                totalAllAchievementCompletions,
                totalSeasonCompletions,
                totalRealmCompletions
        ).thenApply(n -> {
            try {
                clientManager.getLoaded().forEach(client -> {
                    client.getStatContainer().getAchievementCompletions().getAllMap().forEach((key, completion) -> {
                        Integer total = totalAllAchievementCompletions.get(key);
                        if (total == null) {
                            log.error("Error getting all total achievement completions for {}, no total exists ", key.asString(), new RuntimeException("Missing all-time total for achievement: " + key.asString())).submit();
                            total = 1;
                        }
                        completion.setTotalCompletions(total);
                    });
                    client.getStatContainer().getAchievementCompletions().getSeasonMap().forEach((period, map) ->
                            map.forEach((key, completion) -> {
                                Integer total = totalSeasonCompletions.get(period).get(key);
                                if (total == null) {
                                    log.error("Error getting season {} total achievement completions for {}, no total exists ", period, key.asString(), new RuntimeException("Missing season total for achievement: " + key.asString())).submit();
                                    total = 1;
                                }
                                completion.setTotalCompletions(total);
                            }));
                    client.getStatContainer().getAchievementCompletions().getRealmMap().forEach((period, map) ->
                            map.forEach((key, completion) -> {
                                Integer total = totalRealmCompletions.get(period).get(key);
                                if (total == null) {
                                    log.error("Error getting realm {} total achievement completions for {}, no total exists ", period, key.asString(), new RuntimeException("Missing realm total for achievement: " + key.asString())).submit();
                                    total = 1;
                                }
                                completion.setTotalCompletions(total);
                            }));
                });
                return null;
            } catch (Exception e) {
                log.error("Error updating achievement completion totals", e).submit();
                return null;
            }
        });
    }
}