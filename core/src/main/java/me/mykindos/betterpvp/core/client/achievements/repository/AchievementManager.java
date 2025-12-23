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
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.bukkit.NamespacedKey;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@CustomLog
public class AchievementManager extends Manager<NamespacedKey, IAchievement> {

    @Getter
    private final AchievementCategoryManager achievementCategoryManager;

    private final ClientManager clientManager;

    //todo, update this across server instances
    //Period, Achievement, TotalCount
    private ConcurrentMap<NamespacedKey, Integer> totalAllAchievementCompletions = null;
    private ConcurrentMap<Season, ConcurrentMap<NamespacedKey, Integer>> totalSeasonCompletions = null;
    private ConcurrentMap<Realm, ConcurrentMap<NamespacedKey, Integer>> totalRealmCompletions = null;

    private final AchievementCompletionRepository achievementCompletionRepository;

    @Inject
    public AchievementManager(AchievementCategoryManager achievementCategoryManager, AchievementCompletionRepository achievementCompletionRepository, Core core, ClientManager clientManager) {
        this.achievementCategoryManager = achievementCategoryManager;
        this.achievementCompletionRepository = achievementCompletionRepository;
        this.clientManager = clientManager;
        updateTotalAchievementCompletions();
    }


    public CompletableFuture<Void> loadAchievementCompletionsAsync(Client client) {
        return achievementCompletionRepository.loadForContainer(client.getStatContainer()).thenAccept(
                completions -> {
                    completions.asMap().values().forEach(completion -> {
                        int total = getTotalCompletion(getObject(completion.getKey()).orElseThrow(), completion.getPeriod());
                        completion.setTotalCompletions(total);
                    });
                    client.getStatContainer().getAchievementCompletions().fromOther(completions);
                }
        );
    }

    public CompletableFuture<Void> saveCompletion(StatContainer container, IAchievement achievement, Period period) {
        return achievementCompletionRepository.saveCompletion(container, achievement, period)
                .thenAccept(achievementCompletion -> {
                    container.getAchievementCompletions().addCompletion(achievementCompletion);
                    updateTotalCompletions(achievement, achievementCompletion.getPeriod());
                });
    }

    public int getTotalCompletion(IAchievement achievement, Period period) {
        return switch (achievement.getAchievementFilterType()) {
            case ALL ->
                    totalAllAchievementCompletions.get(achievement.getNamespacedKey());
            case SEASON ->
                    totalSeasonCompletions.get((Season) period).get(achievement.getNamespacedKey());
            case REALM ->
                    totalRealmCompletions.get((Realm) period).get(achievement.getNamespacedKey());
        };
    }

    public void updateTotalCompletions(IAchievement achievement, Period period) {
        int total = 0;
        switch (achievement.getAchievementFilterType()) {
            case ALL ->
                total = totalAllAchievementCompletions.compute(achievement.getNamespacedKey(),
                        (key, value) -> value == null ? 1 : value + 1);
            case SEASON ->
                total = totalSeasonCompletions.computeIfAbsent((Season) period, (k) ->
                        new ConcurrentHashMap<>()
                ).compute(achievement.getNamespacedKey(), (key, value) -> value == null ? 1 : value + 1);
            case REALM ->
                total = totalRealmCompletions.computeIfAbsent((Realm) period, (k) ->
                        new ConcurrentHashMap<>()
                ).compute(achievement.getNamespacedKey(), (key, value) -> value == null ? 1 : value + 1);
        }

        final int finalTotal = total;
        clientManager.getLoaded().forEach(client -> {
            client.getStatContainer().getAchievementCompletions().getCompletion(achievement, period).ifPresent(achievementCompletion -> {
                achievementCompletion.setTotalCompletions(finalTotal);
            });
        });
    }


    public CompletableFuture<Void> updateTotalAchievementCompletions() {
        return CompletableFuture.allOf(
                updateTotalAllAchievementCompletions(),
                updateTotalSeasonAchievementCompletions(),
                updateTotalRealmAchievementCompletions()
        );
    }

    private CompletableFuture<Void> updateTotalAllAchievementCompletions() {
        return achievementCompletionRepository.loadTotalAllAchievementCompletions().thenApply(totalCompletions -> {
            totalAllAchievementCompletions = totalCompletions;
            clientManager.getLoaded().forEach(client -> {
                client.getStatContainer().getAchievementCompletions().getAllMap().forEach((key, completion) -> {
                    int total = totalAllAchievementCompletions.get(key);
                    completion.setTotalCompletions(total);
                });
            });
            return null;
        });
    }

    private CompletableFuture<Void> updateTotalSeasonAchievementCompletions() {
        return achievementCompletionRepository.loadTotalSeasonAchievementCompletions().thenApply(totalCompletions -> {
            totalSeasonCompletions = totalCompletions;
            clientManager.getLoaded().forEach(client -> {
                client.getStatContainer().getAchievementCompletions().getSeasonMap().forEach((period, map) -> {
                    map.forEach((key, completion) -> {
                        int total = totalSeasonCompletions.get(period).get(key);
                        completion.setTotalCompletions(total);
                    });
                });
            });
            return null;
        });
    }

    private CompletableFuture<Void> updateTotalRealmAchievementCompletions() {
        return achievementCompletionRepository.loadTotalRealmAchievementCompletions().thenApply(totalCompletions -> {
            totalRealmCompletions = totalCompletions;
            clientManager.getLoaded().forEach(client -> {
                client.getStatContainer().getAchievementCompletions().getRealmMap().forEach((period, map) -> {
                    map.forEach((key, completion) -> {
                        int total = totalRealmCompletions.get(period).get(key);
                        completion.setTotalCompletions(total);
                    });
                });
            });
            return null;
        });
    }
}
