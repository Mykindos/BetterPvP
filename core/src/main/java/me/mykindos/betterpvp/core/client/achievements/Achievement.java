package me.mykindos.betterpvp.core.client.achievements;

import java.lang.reflect.ParameterizedType;
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
import me.mykindos.betterpvp.core.client.achievements.types.AchievementCategory;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * todo
 */
@CustomLog
public abstract class Achievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> implements IAchievement<T, E>, Listener {

    protected static AchievementManager achievementManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(AchievementManager.class);

    @Getter
    private final AchievementCategory achievementCategory;
    @Getter
    @Nullable
    private final NamespacedKey achievementType;
    @Getter
    private final NamespacedKey namespacedKey;
    @Getter
    private final Set<String> watchedProperties = new HashSet<>();
    protected boolean enabled;

    public Achievement( NamespacedKey namespacedKey, AchievementCategory achievementCategory, @Nullable NamespacedKey achievementType, String... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.achievementType = achievementType;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties).toList());
    }

    public Achievement(NamespacedKey namespacedKey, AchievementCategory achievementCategory, @Nullable NamespacedKey achievementType, Enum<?>... watchedProperties) {
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.achievementType = achievementType;
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
     * Returns true if the {@link PropertyContainer} is the same type as {@code T}</t>
     *
     * @param container the {@link PropertyContainer container}
     * @return {@code true} if the container is the same as {@code T}, {@code false} otherwise
     */
    @Override
    public boolean isSameType(PropertyContainer container) {
        //TODO make this an interface check handled lower on the chain
        //I.e. deathachievement implements ICientAchievement with that overriding this function and returning true if container instance of client
        try {
            log.info("Class " +  getClass().getGenericSuperclass().getTypeName()).submit();
            ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
            log.info("Arguments " + Arrays.toString(type.getActualTypeArguments())).submit();
            return IAchievement.super.isSameType(container);
        } catch (Exception e) {
            log.error("Error in isSameType ", e).submit();
            return false;
        }

    }


    @Override
    public void loadConfig(@NotNull String basePath, ExtendedYamlConfiguration config) {
        this.enabled = config.getOrSaveBoolean(basePath + getPath("enabled"), true);

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
