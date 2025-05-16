package me.mykindos.betterpvp.core.client.achievements;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletionRepository;
import me.mykindos.betterpvp.core.client.achievements.types.IAchievement;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
//todo proper async handling (and in repository)
public class AchievementManager extends Manager<IAchievement> {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> achievementCompletions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<NamespacedKey, Integer> totalAchievementCompletions = null;

    private final AchievementCompletionRepository achievementCompletionRepository;

    private final Core core;
    @Inject
    public AchievementManager(AchievementCompletionRepository achievementCompletionRepository, Core core) {
        this.achievementCompletionRepository = achievementCompletionRepository;
        this.core = core;
    }

    @Override
    public void addObject(String identifier, IAchievement object) {
        log.info("loading {}", object.getNamespacedKey().asString()).submit();
        if (getObject(identifier).isPresent()) {
            throw new IllegalArgumentException("Duplicate achievement for type " + identifier);
        }
        super.addObject(identifier, object);
    }

    public void loadContainer(PropertyContainer container) {
        if (totalAchievementCompletions == null) {
            totalAchievementCompletions = achievementCompletionRepository.loadTotalAchievementCompletions();
        }
        ConcurrentHashMap<NamespacedKey, AchievementCompletion> completions = achievementCompletionRepository.loadForContainer(container);
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

    public void unloadId(UUID id) {
        achievementCompletions.remove(id);
    }

    public void saveCompletion(PropertyContainer container, NamespacedKey achievement) {
        AchievementCompletion completion = achievementCompletionRepository.saveCompletion(container, achievement);
        achievementCompletions.get(container.getUniqueId()).put(achievement, completion);
        updateTotalCompletions(achievement);
    }

    public void updateTotalCompletions(NamespacedKey achievement) {
        totalAchievementCompletions.computeIfPresent(achievement, (key, value) -> value + 1);

        achievementCompletions.forEach((id, map) -> {
            map.get(achievement).setTotalCompletions(totalAchievementCompletions.get(achievement));
        });
    }

    Optional<AchievementCompletion> getAchievementCompletion(UUID user, NamespacedKey namespacedKey) {
        return Optional.ofNullable(achievementCompletions.get(user).get(namespacedKey));
    }
}
