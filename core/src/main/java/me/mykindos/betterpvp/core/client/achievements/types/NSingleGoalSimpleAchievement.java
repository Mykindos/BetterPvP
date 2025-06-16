package me.mykindos.betterpvp.core.client.achievements.types;

import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class NSingleGoalSimpleAchievement extends NSimpleAchievement {

    public NSingleGoalSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Double goal, IStat... watchedStats) {
        super(namespacedKey, achievementCategory, achievementType, createMap(goal, watchedStats));
    }

    private static Map<IStat, Double> createMap(Double goal, IStat... watchedStats) {
        Map<IStat, Double> map = new HashMap<>();
        Arrays.stream(watchedStats).forEach(stat -> map.put(stat, goal));
        return map;
    }
}
