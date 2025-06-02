package me.mykindos.betterpvp.core.client.achievements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * todo
 */
@CustomLog
public abstract class Achievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> implements IAchievement<T, E>, Listener {

    protected final static AchievementManager achievementManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(AchievementManager.class);
    protected final static ClientSQLLayer clientSQLLayer = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientSQLLayer.class);
    @Getter
    @Setter
    private String name;
    @Getter
    private final NamespacedKey namespacedKey;
    @Getter
    @Nullable(value = "if has no category, i.e. top level")
    private final NamespacedKey achievementCategory;
    @Getter
    private final Set<String> watchedProperties = new HashSet<>();
    /**
     * A list of float's, where if the old value of {@link Achievement#calculatePercent(Map)} is less than an element and the new value
     * is greater than that element, the player will be notified of their progress
     */
    private List<Float> notifyThresholds;

    protected boolean enabled;
    protected boolean doRewards;

    public Achievement(NamespacedKey namespacedKey, @Nullable NamespacedKey achievementCategory, String... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties).toList());
    }

    public Achievement(NamespacedKey namespacedKey, @Nullable NamespacedKey achievementCategory, Enum<?>... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties)
                .map(Enum::name)
                .toList()
        );
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @Override
    public void onPropertyChangeListener(E event) {
        if (!enabled) return;
        final String changedProperty = event.getProperty();
        final Object newValue = event.getNewValue();
        @Nullable
        final Object oldValue = event.getOldValue();
        final T container = event.getContainer();
        if (!watchedProperties.contains(changedProperty)) return;

        Map<String, Object> otherProperties = new HashMap<>();
        watchedProperties.stream()
                .filter(property -> !property.equals(changedProperty))
                .forEach(property -> {
                    otherProperties.put(property, container.getProperty(property).orElse(0));
                });

        onChangeValue(container, changedProperty, newValue, oldValue, otherProperties);
    }

    @Override
    public void onChangeValue(T container, String property, Object newValue, @Nullable("Null when no previous value") Object oldValue, Map<String, Object> otherProperties) {
        handleNotify(container, property, newValue, oldValue, otherProperties);
        handleComplete(container);
    }

    @Override
    public void loadConfig(@NotNull String basePath, ExtendedYamlConfiguration config) {
        this.enabled = config.getOrSaveBoolean(basePath + getPath("enabled"), true);
        this.notifyThresholds = config.getOrSaveFloatList(basePath + getPath("notifyThresholds"), List.of(0.50f, 0.90f));
        this.doRewards = config.getOrSaveBoolean(basePath + getPath("doRewards"), true);
        //todo load basic information
    }

    protected String getPath(String key) {
        return getNamespacedKey().asString() + "." + key;
    }

    /**
     * Get a progress bar representing the percent complete of this {@link Achievement}
     * @param container the {@link PropertyContainer} this {@link Achievement} is for
     * @return
     */
    protected List<Component> getProgressComponent(T container) {
        float percentage = getPercentComplete(container);
        ProgressBar progressBar = ProgressBar.withProgress(percentage);
        return new ArrayList<>(List.of(progressBar.build()));
    }

    protected List<Component> getCompletionComponent(final T container) {
        final Optional<AchievementCompletion> achievementCompletionOptional = getAchievementCompletion(container);
        if (achievementCompletionOptional.isEmpty()) {
            return new ArrayList<>(List.of());
        }
        final AchievementCompletion achievementCompletion = achievementCompletionOptional.get();

        //todo localize timezones
        final Component timeComponent = Component.text(UtilTime.getDateTime(achievementCompletion.getTimestamp().getTime()), NamedTextColor.GOLD);

        final Component placementComponent = UtilMessage.deserialize("<gold>#%s of %s", achievementCompletion.getCompletedRank(), achievementCompletion.getTotalCompletions());

        return new ArrayList<>(List.of(
                Component.text("Completed", NamedTextColor.GOLD),
                timeComponent,
                placementComponent));
    }

    //TODO do all completion/notification logic here. Define thresholds for notifications, allow overriding of notification methods. Probably should do in onChangeValue here
    //TODO lower classes should define calculatePercent, which is how threshold/completion is determined. Check for passing percent for thresholds

    @Override
    public void notifyProgress(T container, Audience audience, float threshold) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <green>%s</green>% complete",  getName(), getPercentComplete(container)));
    }

    @Override
    public void notifyComplete(T container, Audience audience) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <gold>Completed!",  getName(), getPercentComplete(container)));
    }


    @Override
    public Optional<AchievementCompletion> getAchievementCompletion(T container) {
        return achievementManager.getAchievementCompletion(container.getUniqueId(), namespacedKey);
    }

    @Override
    public void complete(T container) {
        achievementManager.saveCompletion(container, namespacedKey);
    }

    @Override
    public void processRewards(T container) {
        //no rewards by default
    }

    private void handleNotify(T container, String property, Object newValue, @Nullable("Null when no previous value") Object oldValue, Map<String, Object> otherProperties) {
        float oldPercent = calculatePercent(constructMap(property, oldValue == null ? 0 : oldValue, otherProperties));
        float newPercent = calculatePercent(constructMap(property, newValue, otherProperties));
        for (float threshold : notifyThresholds) {
            if (oldPercent < threshold && newPercent > threshold) {
                notifyProgress(container, Bukkit.getPlayer(container.getUniqueId()), threshold);
                return;
            }
        }
    }

    private void handleComplete(T container) {
        Optional<AchievementCompletion> achievementCompletionOptional = getAchievementCompletion(container);
        if (achievementCompletionOptional.isEmpty() && getPercentComplete(container) >= 1.0f) {
            complete(container);
            notifyComplete(container, Bukkit.getPlayer(container.getUniqueId()));
            if (doRewards) {
                processRewards(container);
            }
        }
    }

    private Map<String, Object> constructMap(String property, Object value, Map<String, Object> otherProperties) {
        Map<String, Object> newMap = new HashMap<>(otherProperties);
        newMap.put(property, value);
        return newMap;
    }
}
