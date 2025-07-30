package me.mykindos.betterpvp.core.client.achievements;

import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.types.Achievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                       final IStat stat,
                       final Double newValue,
                       final @Nullable("Null when no previous value") Double oldValue,
                       final Map<IStat, Double> otherStats);

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
     * Gets the priority for sorting this achievement in menus
     * <p>Higher priority achievements are displayed first</p>
     * @param container
     * @param period
     * @return
     */
    default int getPriority(final StatContainer container, final String period) {
        return (int) (1_000_000 - (getPercentComplete(container, period) * 1_000_000));
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     * @param container the {@link PropertyContainer}
     * @return
     */
    default Description getDescription(final StatContainer container, final String period) {
        return Description.builder()
                .icon(getItemProvider(container, period))
                .build();
    }

    /**
     * Gets the default item provider for this achievement. By default, uses
     *
     * @param container
     * @param period
     * @return
     */
    default ItemProvider getItemProvider(final StatContainer container, final String period) {
        return ItemView.builder()
                .material(getMaterial(container, period))
                .customModelData(getCustomModelData(container, period))
                .displayName(getDisplayName(container, period))
                .lore(getLore(container, period))
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();

    }

    /**
     * Gets the display name for the item provider,
     * default white getName
     * @param container
     * @param period
     * @return
     */
    default Component getDisplayName(final StatContainer container, final String period) {
        return Component.text(getName(), NamedTextColor.WHITE);
    }

    /**
     * gets the material for the itemprovider
     * @param container
     * @param period
     * @return
     */
    default Material getMaterial(final StatContainer container, final String period) {
        return Material.PAPER;
    }
    /**
     * gets the custom model data for the itemprovider
     * @param container
     * @param period
     * @return
     */
    default int getCustomModelData(final StatContainer container, final String period) {
        return 0;
    }

    /**
     * A helper method, to easily add a description to the lore
     * without duplicating adding the progress and completion component
     * Used in getLore
     * @param container
     * @param period
     * @return
     */
    default List<String> getStringDescription(final StatContainer container, final String period) {
        return List.of();
    }

    /**
     * Gets the lore for the itemprovider
     * By default, is getStringDescription, getProgressComponent, getCompletionComponent
     * @param container
     * @param period
     * @return
     */
    default List<Component> getLore(final StatContainer container, final String period) {
        List<Component> lore = new ArrayList<>(getStringDescription(container, period).stream()
                .map(UtilMessage::deserialize)
                .toList());
        lore.addAll(this.getProgressComponent(container, period));
        lore.addAll(this.getCompletionComponent(container));
        return lore;
    }


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
     * Get a progress bar representing the percent complete of this {@link Achievement}
     * @param container the {@link PropertyContainer} this {@link Achievement} is for
     * @return
     */
    default List<Component> getProgressComponent(StatContainer container, @Nullable String period) {
        float percentage = getPercentComplete(container, period);
        ProgressBar progressBar = ProgressBar.withProgress(percentage);
        return new ArrayList<>(List.of(progressBar.build()));
    }

    List<Component> getCompletionComponent(StatContainer container);

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
    float calculatePercent(final Map<IStat, Double> propertyMap);

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
                      final IStat stat,
                      final Double newValue,
                      final @Nullable("Null when no previous value") Double oldValue,
                      final Map<IStat, Double> otherStats);

    /**
     * Does the logic for whether to call {@link IAchievement#notifyComplete(PropertyContainer, Audience)} and {@link IAchievement#complete(PropertyContainer)} and executes it
     * @param container
     */
    void handleComplete(final StatContainer container);
}
