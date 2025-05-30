package me.mykindos.betterpvp.core.client.achievements.types;

import me.mykindos.betterpvp.core.client.achievements.Achievement;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.NamespacedKey;

/**
 * Tracks a single property when it is updated, completing at the goal
 * <p>Intermediate Constructors that are for {@link ConfigLoadedAchievement}
 * are expected to have a constructor that can be used with {@link SingleSimpleAchievementConfigLoader#instanstiateAchievement(NamespacedKey, Number)}</p>
 * @param <T> the container type
 * @param <E> the event type
 * @param <C> the {@link SingleSimpleAchievement#goal} type
 */
public abstract class SingleSimpleAchievement <T extends PropertyContainer, E extends PropertyUpdateEvent<T>, C extends Number> extends Achievement<T, E> {

    /**
     * The goal of this achievement, what will be the mark of achieving it
     */
    protected final C goal;

    public SingleSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, C goal, Enum<?> watchedProperty) {
        super(namespacedKey, achievementCategory, watchedProperty);
        this.goal = goal;
    }

    public SingleSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, C goal, String watchedProperty) {
        super(namespacedKey, achievementCategory,watchedProperty);
        this.goal = goal;
    }

    @SuppressWarnings("unchecked")
    protected C getProperty(T container) {
        return (C) container.getProperty(getWatchedProperties().stream().findAny().orElseThrow()).orElseThrow();
    }

    @Override
    public float getPercentComplete(T container) {
        return getProperty(container).floatValue() / goal.floatValue();
    }
}
