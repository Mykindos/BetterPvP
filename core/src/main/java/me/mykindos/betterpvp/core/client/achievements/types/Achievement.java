package me.mykindos.betterpvp.core.client.achievements.types;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * todo
 */
@CustomLog
public abstract class Achievement implements IAchievement, Listener {

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
    private final AchievementType achievementType;
    @Getter
    private final Set<IStat> watchedStats = new HashSet<>();
    /**
     * A list of float's, where if the old value of {@link Achievement#calculatePercent(Map)} is less than an element and the new value
     * is greater than that element, the player will be notified of their progress
     */
    private List<Float> notifyThresholds;

    protected boolean enabled;
    protected boolean doRewards;

    public Achievement(NamespacedKey namespacedKey, @Nullable NamespacedKey achievementCategory, AchievementType achievementType, IStat... watchedStats) {
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.achievementType = achievementType;
        this.watchedStats.addAll(Arrays.stream(watchedStats).toList());
    }

    protected Double getValue(StatContainer container, IStat stat, @Nullable String period) {
        return getAchievementType() == AchievementType.GLOBAL ? stat.getStat(container, StatContainer.GLOBAL_PERIOD) : stat.getStat(container, period);
    }

    protected Double getValue(StatContainer container, IStat stat) {
        return getAchievementType() == AchievementType.GLOBAL ? stat.getStat(container, StatContainer.GLOBAL_PERIOD) : stat.getStat(container, StatContainer.PERIOD);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @Override
    public void onPropertyChangeListener(StatPropertyUpdateEvent event) {
        if (!enabled) return;
        final String changedProperty = event.getStatName();
        final Double newValue = event.getNewValue();
        @Nullable
        final Double oldValue = event.getOldValue();
        final StatContainer container = event.getContainer();

        //validate and retrieve
        final List<IStat> statsTemp = watchedStats.stream()
                .filter(stat -> stat.containsStat(changedProperty))
                .toList();
        if (statsTemp.isEmpty()) return;
        if (statsTemp.size() > 1) {
            throw new IllegalStateException("Expected 1 changed stat, but got " + statsTemp.size() + ". " +
                    "Make sure all watched composite Stats have unique savable stats.");
        }

        final IStat changedStat = statsTemp.getFirst();

        Map<IStat, Double> otherProperties = new HashMap<>();
        watchedStats.stream()
                .filter(stat -> !stat.containsStat(changedProperty))
                .forEach(stat -> {
                    otherProperties.put(stat, getValue(container, stat));
                });

        onChangeValue(container, changedStat, newValue, oldValue, otherProperties);
    }

    @Override
    public void onChangeValue(StatContainer container, IStat stat, Double newValue, @Nullable("Null when no previous value") Double oldValue, Map<IStat, Double> otherProperties) {
        handleNotify(container, stat, newValue, oldValue, otherProperties);
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
    protected List<Component> getProgressComponent(final StatContainer container, @Nullable final String period) {
        float percentage = getPercentComplete(container, period);
        ProgressBar progressBar = ProgressBar.withProgress(percentage);
        return new ArrayList<>(List.of(progressBar.build()));
    }

    protected List<Component> getCompletionComponent(final StatContainer container) {
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
    public void notifyProgress(StatContainer container, Audience audience, float threshold) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <green>%s</green>%% complete",  getName(), UtilFormat.formatNumber(getPercentComplete(container, getPeriod()) * 100)));
    }

    @Override
    public void notifyComplete(StatContainer container, Audience audience) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <gold>Completed!",  getName(), getPercentComplete(container, getPeriod())));
    }

    protected String getPeriod() {
        return getAchievementType() == AchievementType.GLOBAL ? "" : StatContainer.PERIOD;
    }

    @Override
    public Optional<AchievementCompletion> getAchievementCompletion(StatContainer container) {
        return achievementManager.getAchievementCompletion(container.getUniqueId(), namespacedKey, getPeriod());
    }

    @Override
    public void complete(StatContainer container) {
        achievementManager.saveCompletion(container, namespacedKey, getPeriod());
    }

    @Override
    public void processRewards(StatContainer container) {
        //no rewards by default
    }

    @Override
    public void handleNotify(StatContainer container, IStat stat, Double newValue, @Nullable("Null when no previous value") Double oldValue, Map<IStat, Double> otherStats) {
        float oldPercent = calculatePercent(constructMap(stat, oldValue == null ? 0 : oldValue, otherStats));
        float newPercent = calculatePercent(constructMap(stat, newValue, otherStats));
        for (float threshold : notifyThresholds) {
            if (oldPercent < threshold && newPercent >= threshold) {
                notifyProgress(container, Bukkit.getPlayer(container.getUniqueId()), threshold);
                return;
            }
        }
    }

    @Override
    public void handleComplete(StatContainer container) {
        Optional<AchievementCompletion> achievementCompletionOptional = getAchievementCompletion(container);
        if (achievementCompletionOptional.isEmpty() && getPercentComplete(container, getPeriod()) >= 1.0f) {
            complete(container);
            notifyComplete(container, Bukkit.getPlayer(container.getUniqueId()));
            if (doRewards) {
                processRewards(container);
            }
        }
    }

    private Map<IStat, Double> constructMap(IStat stat, Double value, Map<IStat, Double> otherProperties) {
        Map<IStat, Double> newMap = new HashMap<>(otherProperties);
        newMap.put(stat, value);
        return newMap;
    }
}
