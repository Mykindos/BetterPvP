package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.NamespacedKey;

public abstract class NSingleGoalSimpleAchievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> extends NSimpleAchievement<T, E> {

    public NSingleGoalSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory,long goal, Enum<?>... watchedProperties) {
        this(namespacedKey, achievementCategory, goal, Arrays.stream(watchedProperties).map(Enum::name).toArray(String[]::new));
    }

    public NSingleGoalSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory,long goal, String... watchedProperties) {
        super(namespacedKey, achievementCategory, createMap(goal, watchedProperties));
    }

    private static Map<String, Long> createMap(long goal, String... watchedProperties) {
        Map<String, Long> map = new HashMap<>();
        Arrays.stream(watchedProperties).forEach(property -> map.put(property, goal));
        return map;
    }
}
