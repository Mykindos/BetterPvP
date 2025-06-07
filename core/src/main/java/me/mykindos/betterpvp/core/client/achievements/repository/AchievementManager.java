package me.mykindos.betterpvp.core.client.achievements.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
//todo proper async handling (and in repository)
public class AchievementManager extends Manager<IAchievement> {

    @Getter
    private final AchievementCategoryManager achievementCategoryManager;

    private final ConcurrentHashMap<UUID, AchievementCompletionsConcurrentHashMap> achievementCompletions = new ConcurrentHashMap<>();
    //todo, update this across server instances
    //Period, Achievement, TotalCount
    private ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, Integer>> totalAchievementCompletions = null;

    private final AchievementCompletionRepository achievementCompletionRepository;

    @Inject
    public AchievementManager(AchievementCategoryManager achievementCategoryManager, AchievementCompletionRepository achievementCompletionRepository, Core core) {
        this.achievementCategoryManager = achievementCategoryManager;
        this.achievementCompletionRepository = achievementCompletionRepository;
        updateTotalAchievementCompletions();
    }

    @Override
    public void addObject(String identifier, IAchievement object) {
        log.info("loading {}", object.getNamespacedKey().asString()).submit();
        if (getObject(identifier).isPresent()) {
            throw new IllegalArgumentException("Duplicate achievement for type " + identifier);
        }
        super.addObject(identifier, object);
    }

    public CompletableFuture<Void> loadContainer(StatContainer container) {
        return achievementCompletionRepository.loadForContainer(container).thenAccept(
                completions -> {
                    achievementCompletions.put(container.getUniqueId(), completions);
                    completions.forEach(completion -> {
                        int total = totalAchievementCompletions.get(completion.getPeriod()).get(completion.getKey());
                        completion.setTotalCompletions(total);
                    });
                }
        );
    }

    public void unloadId(UUID id) {
        achievementCompletions.remove(id);
    }

    public CompletableFuture<Void> saveGlobalCompletion(StatContainer container, NamespacedKey namespacedKey) {
        return saveCompletion(container, namespacedKey, "");
    }

    public CompletableFuture<Void> saveCompletion(StatContainer container, NamespacedKey achievement, String period) {
        return achievementCompletionRepository.saveCompletion(container, achievement, period)
                .thenAccept(achievementCompletion -> {
                    achievementCompletions.get(container.getUniqueId()).addCompletion(achievementCompletion);
                    updateTotalCompletions(achievement, achievementCompletion.getPeriod());
                });

    }

    public void updateTotalCompletions(NamespacedKey achievement, String period) {
        totalAchievementCompletions.compute(period, (k, v) -> {
                    if (v == null) {
                        v = new ConcurrentHashMap<>();
                    }
                    v.compute(achievement, (key, value) -> value == null ? 1 : value + 1);
                    return v;
                });
        final int total = totalAchievementCompletions.get(period).get(achievement);

        achievementCompletions.forEach((id, map) ->
            map.getCompletion(achievement, period).ifPresent(achievementCompletion -> achievementCompletion.setTotalCompletions(total))
        );
    }

    public Optional<AchievementCompletion> getAchievementCompletion(UUID user, NamespacedKey namespacedKey, String period) {
        return achievementCompletions.get(user).getCompletion(namespacedKey, period);
    }

    public CompletableFuture<Void> updateTotalAchievementCompletions() {
        return achievementCompletionRepository.loadTotalAchievementCompletions().thenApply(totalCompletions -> {
            totalAchievementCompletions = totalCompletions;
            totalAchievementCompletions.forEach((period, achievementTotals) -> {
                achievementTotals.forEach((achievement, total) -> {
                    achievementCompletions.forEach((id, map) -> {
                        map.getCompletion(achievement, period)
                                .ifPresent(completion -> completion.setTotalCompletions(total));
                    });
                });
            });
            return null;
        });
    }
}
