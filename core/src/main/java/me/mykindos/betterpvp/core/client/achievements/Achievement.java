package me.mykindos.betterpvp.core.client.achievements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.types.IAchievement;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * todo
 */
@CustomLog
public abstract class Achievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> implements IAchievement<T, E>, Listener {

    protected static AchievementManager achievementManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(AchievementManager.class);

    @Getter
    private final NamespacedKey namespacedKey;
    @Getter
    private final Set<String> watchedProperties = new HashSet<>();
    protected boolean enabled;

    public Achievement(NamespacedKey namespacedKey, String... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties).toList());
    }

    public Achievement(NamespacedKey namespacedKey, Enum<?>... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties)
                .map(Enum::name)
                .toList()
        );
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @Override
    public void onPropertyChangeListener(E event) {
        if (!enabled) return;
        log.info(event.toString()).submit();
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
                    otherProperties.put(property, container.getProperty(property).orElseThrow());
                });

        onChangeValue(container, changedProperty, newValue, oldValue, otherProperties);
    }

    /**
     * Load this {@link IAchievement} from the {@link ExtendedYamlConfiguration config}
     * @param config the {@link ExtendedYamlConfiguration config} for the achievement (expected "achievements")
     * @apiNote It is expected that overrides to this function call the {@code super} function
     */
    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        this.enabled = config.getOrSaveBoolean(getPath("enabled"), true);

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
        return List.of(progressBar.build());
    }

    @Override
    public Optional<AchievementCompletion> getAchievementCompletion(T container) {
        return achievementManager.getAchievementCompletion(container.getUniqueId(), namespacedKey);
    }

    @Override
    public void complete(T container) {
        achievementManager.saveCompletion(container, namespacedKey);
    }
}
