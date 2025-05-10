package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Map;
import java.util.Optional;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IAchievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> {


    /**
     * Listens for the event to call {@link IAchievement#onChangeValue(PropertyContainer, String, Object, Object, Map)} if valid
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
     * Get the {@link NamespacedKey} for this achievement
     * @return the {@link NamespacedKey}
     */
    NamespacedKey getNamespacedKey();

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

    /**
     * Load this {@link IAchievement} from the {@link ExtendedYamlConfiguration config}
     * @param config the {@link ExtendedYamlConfiguration config} for the achievement (expected "achievements")
     * @apiNote It is expected that overrides to this function call the {@code super} function
     * @see IAchievement#loadConfig(String, ExtendedYamlConfiguration)
     */
    default void loadConfig(ExtendedYamlConfiguration config) {
        loadConfig("", config);
    }

    /**
     *  Load this {@link IAchievement} from the {@link ExtendedYamlConfiguration config} starting at the base path
     * @param basePath the base path terminated by a {@code "."} unless value is {@code ""}
     * @param config the {@link ExtendedYamlConfiguration config} for the achievement (expected "achievements")
     * @apiNote It is expected that overrides to this function call the {@code super} function
     */
    void loadConfig(@NotNull String basePath, ExtendedYamlConfiguration config);

    /**
     * Get when this {@link IAchievement} was completed for the {@link PropertyContainer}
     * @param container the {@link PropertyContainer}
     * @return an {@link Optional} of {@link AchievementCompletion} if this achievement has been completed or
     * {@link Optional#empty() empty} if not
     */
    Optional<AchievementCompletion> getAchievementCompletion(T container);

    /**
     * Complete this {@link IAchievement} for the given {@link PropertyContainer}
     * @param container the {@link PropertyContainer}
     */
    void complete(T container);
}
