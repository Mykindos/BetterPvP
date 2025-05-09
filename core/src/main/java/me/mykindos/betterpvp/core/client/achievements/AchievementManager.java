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
public class AchievementManager extends Manager<IAchievement> {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> achievementCompletions = new ConcurrentHashMap<>();

    private final AchievementCompletionRepository achievementCompletionRepository;

    private final Core core;
    @Inject
    public AchievementManager(AchievementCompletionRepository achievementCompletionRepository, Core core) {
        this.achievementCompletionRepository = achievementCompletionRepository;
        this.core = core;
    }

    @Override
    public void addObject(String identifier, IAchievement object) {
        if (getObject(identifier).isPresent()) {
            throw new IllegalArgumentException("Duplicate achievement for type " + identifier);
        }
        super.addObject(identifier, object);
    }

    public void loadContainer(PropertyContainer container) {
        ConcurrentHashMap<NamespacedKey, AchievementCompletion> completions = achievementCompletionRepository.loadForContainer(container);
        achievementCompletions.compute(container.getUniqueId(),
                (key, value) -> {
                    if (value == null) {
                        return completions;
                    }
                    value.putAll(completions);
                    return value;
                });
    }

    public void unloadId(UUID id) {
        achievementCompletions.remove(id);
    }

    public void saveCompletion(PropertyContainer container, NamespacedKey achievement) {
        AchievementCompletion completion = achievementCompletionRepository.saveCompletion(container, achievement);
        achievementCompletions.get(container.getUniqueId()).put(achievement, completion);
    }

    Optional<AchievementCompletion> getAchievementCompletion(UUID user, NamespacedKey namespacedKey) {
        return Optional.ofNullable(achievementCompletions.get(user).get(namespacedKey));
    }
}
