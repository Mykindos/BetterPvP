package me.mykindos.betterpvp.core.client.achievements.types;

import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class NSingleGoalSimpleAchievement extends NSimpleAchievement {

    protected NSingleGoalSimpleAchievement(String name, NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Long goal, IStat... watchedStats) {
        super(name, namespacedKey, achievementCategory, achievementType, createMap(goal, watchedStats));
    }

    private static Map<IStat, Long> createMap(Long goal, IStat... watchedStats) {
        Map<IStat, Long> map = new HashMap<>();
        Arrays.stream(watchedStats).forEach(stat -> map.put(stat, goal));
        return map;
    }
}
