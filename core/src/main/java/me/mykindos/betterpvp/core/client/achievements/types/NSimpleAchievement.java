package me.mykindos.betterpvp.core.client.achievements.types;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
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

    protected NSimpleAchievement(String name, NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Map<IStat, Long> statGoals) {
        super(name, namespacedKey, achievementCategory, achievementType, statGoals.keySet().toArray(IStat[]::new));
        this.statGoals = new HashMap<>(statGoals);
    }

    @Override
    public float getPercentComplete(StatContainer container, @Nullable String period) {

        //todo abstract getting this property map
        Map<IStat, Long> propertyMap = new HashMap<>();
        for (IStat property : getWatchedStats()) {
            Long value = getValue(container, property, period);
            propertyMap.put(property, value);
        }
        return Math.clamp(calculatePercent(propertyMap), 0.0f, 1.0f);
    }

    @Override
    public int getPriority(StatContainer container, String period) {
        int previousPriority = super.getPriority(container, period);
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

    /* //TODO
    @Override
    protected List<Component> getProgressComponent(T container) {
        C current = getProperty(container);
        List<Component> progressComponent = new ArrayList<>(super.getProgressComponent(container));
        Component bar = progressComponent.getFirst();
        progressComponent.removeFirst();
        progressComponent.addFirst(bar.append(UtilMessage.deserialize(" (<green>%s</green>/<yellow>%s</yellow>)", current, goal)));
        return progressComponent;
    }*/
}
