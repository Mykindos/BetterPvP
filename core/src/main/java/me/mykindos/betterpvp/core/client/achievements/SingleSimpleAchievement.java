package me.mykindos.betterpvp.core.client.achievements;

import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

/**
 * Tracks a single property when it is updated, completing at the goal
 * @param <T>
 * @param <E>
 * @param <C>
 */
public abstract class SingleSimpleAchievement <T extends PropertyContainer, E extends PropertyUpdateEvent<T>, C extends Number> extends Achievement<T, E> {
    protected final C goal;

    public SingleSimpleAchievement(String name, C goal, Enum<?> watchedProperty) {
        super(name, watchedProperty);
        this.goal = goal;
    }

    public SingleSimpleAchievement(String name, C goal, String watchedProperty) {
        super(name, watchedProperty);
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
