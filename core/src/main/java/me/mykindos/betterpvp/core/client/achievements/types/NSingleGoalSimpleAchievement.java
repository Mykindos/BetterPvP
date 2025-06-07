package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import org.bukkit.NamespacedKey;

public abstract class NSingleGoalSimpleAchievement extends NSimpleAchievement {

    public NSingleGoalSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Double goal, Enum<?>... watchedProperties) {
        this(namespacedKey, achievementCategory, achievementType, goal, Arrays.stream(watchedProperties).map(java.lang.Enum::name).toArray(String[]::new));
    }

    public NSingleGoalSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Double goal, String... watchedProperties) {
        super(namespacedKey, achievementCategory, achievementType, createMap(goal, watchedProperties));
    }

    private static Map<String, Double> createMap(Double goal, String... watchedProperties) {
        Map<String, Double> map = new HashMap<>();
        Arrays.stream(watchedProperties).forEach(property -> map.put(property, goal));
        return map;
    }
}
