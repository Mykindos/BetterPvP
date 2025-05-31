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
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.achievements.types.IAchievement;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
//todo proper async handling (and in repository)
public class AchievementManager extends Manager<IAchievement> {

    @Getter
    private final AchievementCategoryManager achievementCategoryManager;

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> achievementCompletions = new ConcurrentHashMap<>();
    //todo, update this across server instances
    private ConcurrentHashMap<NamespacedKey, Integer> totalAchievementCompletions = null;

    private final AchievementCompletionRepository achievementCompletionRepository;

    @Inject
    public AchievementManager(AchievementCategoryManager achievementCategoryManager, AchievementCompletionRepository achievementCompletionRepository, Core core) {
        this.achievementCategoryManager = achievementCategoryManager;
        this.achievementCompletionRepository = achievementCompletionRepository;
    }

    @Override
    public void addObject(String identifier, IAchievement object) {
        log.info("loading {}", object.getNamespacedKey().asString()).submit();
        if (getObject(identifier).isPresent()) {
            throw new IllegalArgumentException("Duplicate achievement for type " + identifier);
        }
        super.addObject(identifier, object);
    }

    public CompletableFuture<Void> loadContainer(PropertyContainer container) {
        if (totalAchievementCompletions == null) {
            //todo update periodically to fetch updates from other servers, async
            updateTotalAchievementCompletions().join();
        }
        return achievementCompletionRepository.loadForContainer(container).thenAccept(
                completions -> {
                    achievementCompletions.compute(container.getUniqueId(),
                            (key, value) -> {
                                if (value == null) {
                                    return completions;
                                }
                                value.putAll(completions);
                                return value;
                            });
                    achievementCompletions.get(container.getUniqueId()).forEach((key, achievementCompletion) -> achievementCompletion.setTotalCompletions(totalAchievementCompletions.get(key)));
                }
        );
    }

    public void unloadId(UUID id) {
        achievementCompletions.remove(id);
    }

    public CompletableFuture<Void> saveCompletion(PropertyContainer container, NamespacedKey achievement) {
        return achievementCompletionRepository.saveCompletion(container, achievement)
                .thenAccept(achievementCompletion -> {
                    achievementCompletions.get(container.getUniqueId()).put(achievement, achievementCompletion);
                    updateTotalCompletions(achievement);
                });

    }

    public void updateTotalCompletions(NamespacedKey achievement) {
        totalAchievementCompletions.computeIfPresent(achievement, (key, value) -> value + 1);

        achievementCompletions.forEach((id, map) -> {
            map.get(achievement).setTotalCompletions(totalAchievementCompletions.get(achievement));
        });
    }

    public Optional<AchievementCompletion> getAchievementCompletion(UUID user, NamespacedKey namespacedKey) {
        return Optional.ofNullable(achievementCompletions.get(user).get(namespacedKey));
    }

    public CompletableFuture<Void> updateTotalAchievementCompletions() {
        return achievementCompletionRepository.loadTotalAchievementCompletions().thenApply(totalCompletions -> {
            totalAchievementCompletions = totalCompletions;
            totalAchievementCompletions.forEach((achievement, total) -> {
                achievementCompletions.forEach((id, map) -> {
                    map.get(achievement).setTotalCompletions(total);
                });
            });
            return null;
        });
    }
}
