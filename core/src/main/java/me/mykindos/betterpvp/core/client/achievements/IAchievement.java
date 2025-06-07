package me.mykindos.betterpvp.core.client.achievements;

import java.util.Map;
import java.util.Optional;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.audience.Audience;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @param <T>
 * @param <E>
 */
public interface IAchievement {

    /**
     * Get the simple name of this achievement
     * @return
     */
    String getName();

    /**
     * Listens for the event to call {@link IAchievement#onChangeValue(PropertyContainer, String, Object, Object, Map)} if valid
     * @param event
     */
    void onPropertyChangeListener(final StatPropertyUpdateEvent event);

    /**
     * Called when a watched property is updated
     * @param container the {@link PropertyContainer} being updated
     * @param property the property that has changed
     * @param newValue the value that was changed
     * @param oldValue the previous value, {@code null} when there is no previous value
     * @param otherProperties the other watched properties for the container, excluding the changed property
     */
    void onChangeValue(final StatContainer container,
                       final String property,
                       final Double newValue,
                       final @Nullable("Null when no previous value") Double oldValue,
                       final Map<String, Double> otherProperties);

    /**
     * Get the {@link AchievementCategory} of this {@link IAchievement}
     * @return the {@link AchievementCategory}
     */
    NamespacedKey getAchievementCategory();

    /**
     * Get the AchievementType of this {@link IAchievement}
     * @return
     */
    AchievementType getAchievementType();

    /**
     * Get the {@link NamespacedKey} for this achievement
     * @return the {@link NamespacedKey}
     */
    @NotNull
    NamespacedKey getNamespacedKey();

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     * @param container the {@link PropertyContainer}
     * @return
     */
    Description getDescription(final StatContainer container, @Nullable final String period);

    /**
     * For the given {@link PropertyContainer}, calculate how complete this achievement is
     * @param container the {@link PropertyContainer}
     * @return between {@code 0.0f} (no progress) and {@code 1.0f} (completed)
     */
    float getPercentComplete(final StatContainer container, @Nullable final String period);

    /**
     * Load this {@link IAchievement} from the {@link ExtendedYamlConfiguration config}
     * @param config the {@link ExtendedYamlConfiguration config} for the achievement (expected "achievements")
     * @apiNote It is expected that overrides to this function call the {@code super} function
     * @see IAchievement#loadConfig(String, ExtendedYamlConfiguration)
     */
    default void loadConfig(final ExtendedYamlConfiguration config) {
        loadConfig("", config);
    }

    /**
     *  Load this {@link IAchievement} from the {@link ExtendedYamlConfiguration config} starting at the base path
     * @param basePath the base path terminated by a {@code "."} unless value is {@code ""}
     * @param config the {@link ExtendedYamlConfiguration config} for the achievement (expected "achievements")
     * @apiNote It is expected that overrides to this function call the {@code super} function
     */
    void loadConfig(@NotNull final String basePath, final ExtendedYamlConfiguration config);

    /**
     * Notify the player of their progress
     * @param container
     * @param audience
     */
    void notifyProgress(final StatContainer container, final Audience audience, final float threshold);
    void notifyComplete(final StatContainer container, final Audience audience);

    /**
     * Given the propertyMap, evaluate how complete this achievement is or would be
     * @param propertyMap
     * @return
     */
    float calculatePercent(final Map<String, Double> propertyMap);

    /**
     * Get when this {@link IAchievement} was completed for the {@link PropertyContainer}
     * @param container the {@link PropertyContainer}
     * @return an {@link Optional} of {@link AchievementCompletion} if this achievement has been completed or
     * {@link Optional#empty() empty} if not
     */
    Optional<AchievementCompletion> getAchievementCompletion(final StatContainer container);

    /**
     * Complete this {@link IAchievement} for the given {@link PropertyContainer}
     * @param container the {@link PropertyContainer}
     */
    void complete(final StatContainer container);

    /**
     * Gives the rewards for this achievement on completion
     * @param container
     */
    void processRewards(final StatContainer container);

    /**
     * Does the logic for whether to call {@link IAchievement#notifyProgress(PropertyContainer, Audience, float)} and executes it
     * @param container
     * @param property
     * @param newValue
     * @param oldValue
     * @param otherProperties
     */
    void handleNotify(final StatContainer container,
                      final String property,
                      final Double newValue,
                      final @Nullable("Null when no previous value") Double oldValue,
                      final Map<String, Double> otherProperties);

    /**
     * Does the logic for whether to call {@link IAchievement#notifyComplete(PropertyContainer, Audience)} and {@link IAchievement#complete(PropertyContainer)} and executes it
     * @param container
     */
    void handleComplete(final StatContainer container);
}
