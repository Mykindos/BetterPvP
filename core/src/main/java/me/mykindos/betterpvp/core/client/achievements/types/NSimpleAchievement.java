package me.mykindos.betterpvp.core.client.achievements.types;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks multiple properties on update. When all propertyGoals are met, this achievement completes
 * <p>Intermediate Constructors that are for {@link NoReflection}
 * are expected to have a constructor that can be used with {@link SingleSimpleAchievementConfigLoader#instanstiateAchievement(NamespacedKey, Number)}</p>
 * @param <T> the container type
 * @param <E> the event type
 */
@CustomLog
public abstract class NSimpleAchievement extends Achievement {

    /**
     * The goal of this achievement, what will be the mark of achieving it
     */
    protected Map<IStat, Long> statGoals;

    protected NSimpleAchievement(String name, NamespacedKey namespacedKey, NamespacedKey achievementCategory, StatFilterType achievementFilterType, Map<IStat, Long> statGoals) {
        super(name, namespacedKey, achievementCategory, achievementFilterType, statGoals.keySet().toArray(IStat[]::new));
        this.statGoals = new HashMap<>(statGoals);
    }

    @Override
    public float getPercentComplete(StatContainer container, StatFilterType type, @Nullable Period period) {

        //todo abstract getting this property map
        Map<IStat, Long> propertyMap = getPropertyMap(container, type, period);
        return Math.clamp(calculatePercent(propertyMap), 0.0f, 1.0f);
    }

    public Map<IStat, Long> getPropertyMap(StatContainer container, StatFilterType type, @Nullable Period period) {
        Map<IStat, Long> propertyMap = new HashMap<>();
        for (IStat stat : getWatchedStats()) {
            Long value = getValue(container, stat, type, period);
            propertyMap.put(stat, value);
        }
        return propertyMap;
    }

    @Override
    public int getPriority(StatContainer container, StatFilterType type, Period period) {
        int previousPriority = super.getPriority(container, type, period);
        if (previousPriority < 1_000_000) {
            return previousPriority;
        }
        return (int) statGoals.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    @Override
    public float calculatePercent(Map<IStat, Long> statMap) {
        long total = statGoals.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long current = statMap.entrySet().stream()
                .mapToLong(entrySet -> {
                    long localTotal = statGoals.get(entrySet.getKey());
                    return Math.min(localTotal, entrySet.getValue());
                }).sum();
        return ((float) current/total);
    }

    public float calculateCurrentElementPercent(StatContainer statContainer, IStat iStat) {
        long goal = statGoals.get(iStat);
        long current = getValue(statContainer, iStat);
        return (float) Math.clamp((double) current /goal, 0.0, 1.0);
    }
}
