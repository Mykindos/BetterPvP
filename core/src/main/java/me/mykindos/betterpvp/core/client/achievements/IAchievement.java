package me.mykindos.betterpvp.core.client.achievements;

import java.util.Map;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.jetbrains.annotations.Nullable;

public interface IAchievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> {


    /**
     * Listens for the event to call {@link IAchievement#onChangeValue(PropertyContainer, String, Object, Map)} if valid
     * @param event
     */
    void onPropertyChangeListener(E event);

    /**
     * Called when a watched property is updated
     * @param container the {@link PropertyContainer} being updated
     * @param property the property that has changed
     * @param newValue the value that was changed
     * @param oldValue the previous value, {@code null} when there is no previous value
     * @param otherProperties the other watched properties for the container, excluding the changed property
     */
    void onChangeValue(T container, String property, Object newValue, @Nullable("Null when no previous value") Object oldValue, Map<String, Object> otherProperties);

    /**
     * Gets the name of this achievement
     * @return the name
     */
    String getName();


    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     * @param container the {@link PropertyContainer}
     * @return
     */
    Description getDescription(T container);

    /**
     * For the given {@link PropertyContainer}, calculate how complete this achievement is
     * @param container the {@link PropertyContainer}
     * @return between {@code 0.0f} (no progress) and {@code 1.0f} (completed)
     */
    float getPercentComplete(T container);
}
