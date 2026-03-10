package me.mykindos.betterpvp.core.client.achievements.types;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an Achievement. Achievements are for a certain period (ALL, meaning stats across all realms,
 * SEASON, stats across all realms that have a speicific season, and REALM, for the specific realm
 */
@CustomLog
public abstract class Achievement implements IAchievement, Listener, IStat {

    protected static final AchievementManager achievementManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(AchievementManager.class);
    @Getter
    @Setter
    private String name;
    @Getter
    private final NamespacedKey namespacedKey;
    @Getter
    @Nullable(value = "if has no category, i.e. top level")
    private final NamespacedKey achievementCategory;
    @Getter
    private final StatFilterType achievementFilterType;
    @Getter
    private final Set<IStat> watchedStats = new HashSet<>();
    /**
     * A list of float's, where if the old value of {@link Achievement#calculatePercent(Map)} is less than an element and the new value
     * is greater than that element, the player will be notified of their progress
     */
    private List<Float> notifyThresholds;

    protected boolean enabled;
    protected boolean doRewards;

    protected Achievement(String name, NamespacedKey namespacedKey, @Nullable NamespacedKey achievementCategory, StatFilterType achievementFilterType, IStat... watchedStats) {
        this.name = name;
        this.namespacedKey = namespacedKey;
        this.achievementCategory = achievementCategory;
        this.achievementFilterType = achievementFilterType;
        this.watchedStats.addAll(Arrays.stream(watchedStats).toList());
    }

    protected Long getValue(StatContainer container, IStat stat, StatFilterType type, @Nullable("When type is ALL") Period period) {
        return stat.getStat(container, type, period);
    }

    /**
     * Gets the value of this iStat based off of this achievement's AchievementType
     * @param container
     * @param stat
     * @return
     */
    protected Long getValue(StatContainer container, IStat stat) {
        return switch (getAchievementFilterType()) {
            case ALL ->
                stat.getStat(container, StatFilterType.ALL, null);
            case SEASON ->
                stat.getStat(container, StatFilterType.SEASON, Core.getCurrentRealm().getSeason());
            case REALM ->
                stat.getStat(container, StatFilterType.REALM, Core.getCurrentRealm());
        };
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @Override
    public void onPropertyChangeListener(final StatPropertyUpdateEvent event) {
        if (!enabled) return;
        try {
            final IStat stat = event.getStat();
            final Long newValue = event.getNewValue();
            @Nullable
            final Long oldValue = event.getOldValue();
            final StatContainer container = event.getContainer();
            //validate and retrieve
            final List<IStat> statsTemp = watchedStats.stream()
                    .filter(iStat -> iStat.containsStat(stat))
                    .toList();

            if (statsTemp.isEmpty()) return;
            if (statsTemp.size() > 1) {
                throw new IllegalStateException("Expected 1 changed stat, but got " + statsTemp.size() + ". " +
                        "Make sure all watched composite Stats have unique savable stats.");
            }

            final IStat changedStat = statsTemp.getFirst();

            Map<IStat, Long> otherProperties = new HashMap<>();
            watchedStats.stream()
                    .filter(iStat -> !stat.containsStat(stat))
                    .forEach(iStat -> {
                        otherProperties.put(stat, getValue(container, stat));
                    });

            onChangeValue(container, changedStat, newValue, oldValue, otherProperties);
        } catch (Exception e) {
            log.error("Error looking to update an achievement {}", getName(), e).submit();
        }


    }

    @Override
    public void onChangeValue(StatContainer container, IStat stat, Long newValue, @Nullable("Null when no previous value") Long oldValue, Map<IStat, Long> otherProperties) {
        handleNotify(container, stat, newValue, oldValue, otherProperties);
        handleComplete(container);
        float oldPercent = calculatePercent(constructMap(stat, oldValue == null ? 0 : oldValue, otherProperties));
        float newPercent = calculatePercent(constructMap(stat, newValue, otherProperties));
        long oldLong = (long) (oldPercent * FP_MODIFIER);
        long newLong = (long) (newPercent * FP_MODIFIER);
        log.info("achievement change {}", getName()).submit();
        new StatPropertyUpdateEvent(container, this, newLong,oldLong).callEvent();
    }

    @Override
    public void loadConfig(@NotNull String basePath, ExtendedYamlConfiguration config) {
        this.enabled = config.getOrSaveBoolean(basePath + getPath("enabled"), true);
        this.notifyThresholds = config.getOrSaveFloatList(basePath + getPath("notifyThresholds"), List.of(0.50f, 0.90f));
        this.doRewards = config.getOrSaveBoolean(basePath + getPath("doRewards"), true);
    }

    protected String getPath(String key) {
        return getNamespacedKey().asString() + "." + key;
    }

    @Override
    public List<Component> getCompletionComponent(final StatContainer container) {
        final Optional<AchievementCompletion> achievementCompletionOptional = getAchievementCompletion(container);
        if (achievementCompletionOptional.isEmpty()) {
            return new ArrayList<>(List.of());
        }
        final AchievementCompletion achievementCompletion = achievementCompletionOptional.get();

        //todo localize timezones
        final Component timeComponent = Component.text(UtilTime.getDateTime(achievementCompletion.getTimestamp()), NamedTextColor.GOLD);

        final Component placementComponent = UtilMessage.deserialize("<gold>#%s of %s", achievementCompletion.getCompletedRank() + 1, achievementCompletion.getTotalCompletions());

        return new ArrayList<>(List.of(
                Component.text("Completed", NamedTextColor.GOLD),
                timeComponent,
                placementComponent));
    }

    @Override
    public void notifyProgress(StatContainer container, Audience audience, float threshold) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <green>%s</green>%% complete",  getName(), UtilFormat.formatNumber(getPercentComplete(container, achievementFilterType, getPeriod()) * 100)));
    }

    @Override
    public void notifyComplete(StatContainer container, Audience audience) {
        UtilMessage.message(audience, "Achievement", UtilMessage.deserialize("<white>%s: <gold>Completed!",  getName(), getPercentComplete(container, achievementFilterType, getPeriod())));
    }

    protected Period getPeriod() {
        return switch (getAchievementFilterType()) {
            case ALL ->
                    null;
            case SEASON ->
                    Core.getCurrentRealm().getSeason();
            case REALM ->
                    Core.getCurrentRealm();
        };
    }

    @Override
    public Optional<AchievementCompletion> getAchievementCompletion(StatContainer container) {
        return container.getAchievementCompletions().getCompletion(this, getPeriod());
    }

    @Override
    public void complete(StatContainer container) {
        achievementManager.saveCompletion(container, this, getPeriod());
    }

    @Override
    public void processRewards(StatContainer container) {
        //no rewards by default
    }

    @Override
    public void handleNotify(StatContainer container, IStat stat, Long newValue, @Nullable("Null when no previous value") Long oldValue, Map<IStat, Long> otherStats) {
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
        if (achievementCompletionOptional.isEmpty() && getPercentComplete(container, achievementFilterType, getPeriod()) >= 1.0f) {
            complete(container);
            notifyComplete(container, Bukkit.getPlayer(container.getUniqueId()));
            if (doRewards) {
                processRewards(container);
            }
        }
    }

    private Map<IStat, Long> constructMap(IStat stat, Long value, Map<IStat, Long> otherProperties) {
        Map<IStat, Long> newMap = new HashMap<>(otherProperties);
        newMap.put(stat, value);
        return newMap;
    }

    /**
     * Get the stat represented by this object from the statContainer.
     * period object must be the correct type as defined by the type
     *
     * @param statContainer the statContainer to source the value from
     * @param type          what type of period is being fetched from
     * @param period        The period being fetched from, must be {@link Realm} or {@link Season} if type is not ALL
     * @return the stat value represented by this stat
     */
    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        return (long) (getPercentComplete(statContainer, type, period) * IStat.FP_MODIFIER);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public @NotNull StatValueType getStatValueType() {
        return StatValueType.DOUBLE;
    }

    @Override
    public @NotNull String getStatType() {
        return namespacedKey.asString();
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return null;
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return getName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return false;
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        return this.equals(otherStat);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return this;
    }
}
