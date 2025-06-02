package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.HashMap;
import java.util.Map;
import me.mykindos.betterpvp.core.client.achievements.Achievement;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.NamespacedKey;

/**
 * Tracks multiple properties on update. When all propertyGoals are met, this achievement completes
 * <p>Intermediate Constructors that are for {@link ConfigLoadedAchievement}
 * are expected to have a constructor that can be used with {@link SingleSimpleAchievementConfigLoader#instanstiateAchievement(NamespacedKey, Number)}</p>
 * @param <T> the container type
 * @param <E> the event type
 */
public abstract class NSimpleAchievement <T extends PropertyContainer, E extends PropertyUpdateEvent<T>> extends Achievement<T, E> {

    /**
     * The goal of this achievement, what will be the mark of achieving it
     */
    protected Map<String, Long> propertyGoals;

    public NSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, Map<String, Long> propertyGoals) {
        super(namespacedKey, achievementCategory, propertyGoals.keySet().toArray(new String[0]));
        this.propertyGoals = new HashMap<>(propertyGoals);
    }

    @Override
    public float getPercentComplete(T container) {

        //todo abstract getting this property map
        Map<String, Object> propertyMap = new HashMap<>();
        for (String property : getWatchedProperties()) {
            propertyMap.put(property, container.getProperty(property).orElse(0));
        }
        return Math.clamp(calculatePercent(propertyMap), 0.0f, 1.0f);
    }

    @Override
    public float calculatePercent(Map<String, Object> propertyMap) {
        long total = propertyGoals.values().stream()
                .mapToLong(PropertyContainer::forceNumber)
                .sum();
        long current = propertyMap.entrySet().stream()
                .mapToLong(entrySet -> {
                    long localTotal = propertyGoals.get(entrySet.getKey());
                    return Math.max(localTotal, PropertyContainer.forceNumber(entrySet.getValue()));
                }).sum();


        return (float) current/total;
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
