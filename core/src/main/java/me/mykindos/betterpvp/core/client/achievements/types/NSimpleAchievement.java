package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks multiple properties on update. When all propertyGoals are met, this achievement completes
 * <p>Intermediate Constructors that are for {@link ConfigLoadedAchievement}
 * are expected to have a constructor that can be used with {@link SingleSimpleAchievementConfigLoader#instanstiateAchievement(NamespacedKey, Number)}</p>
 * @param <T> the container type
 * @param <E> the event type
 */
@CustomLog
public abstract class NSimpleAchievement extends Achievement {

    /**
     * The goal of this achievement, what will be the mark of achieving it
     */
    protected Map<String, Double> propertyGoals;

    public NSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Map<String, Double> propertyGoals) {
        super(namespacedKey, achievementCategory, achievementType, propertyGoals.keySet().toArray(String[]::new));
        this.propertyGoals = new HashMap<>(propertyGoals);
    }

    @Override
    public float getPercentComplete(StatContainer container, @Nullable String period) {

        //todo abstract getting this property map
        Map<String, Double> propertyMap = new HashMap<>();
        for (String property : getWatchedProperties()) {
            propertyMap.put(property, getValue(container, property, period));
        }
        return Math.clamp(calculatePercent(propertyMap), 0.0f, 1.0f);
    }

    @Override
    public float calculatePercent(Map<String, Double> propertyMap) {
        double total = propertyGoals.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        double current = propertyMap.entrySet().stream()
                .mapToDouble(entrySet -> {
                    double localTotal = propertyGoals.get(entrySet.getKey());
                    return Math.min(localTotal, entrySet.getValue());
                }).sum();
        return (float) ((float) current/total);
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
