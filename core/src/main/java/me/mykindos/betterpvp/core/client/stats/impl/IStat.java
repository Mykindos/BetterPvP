package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.function.Predicate;
public interface IStat {
    /**
     * The amount that {@link Double} or {@link Float} values are multiplied then cast to long by
     */
    long FP_MODIFIER = 1000L;

    /**
     * Get the stat represented by this object from the statContainer.
     * period object must be the correct type as defined by the type
     * @param statContainer the statContainer to source the value from
     * @param type what type of period is being fetched from
     * @param period The period being fetched from, must be {@link Realm} or {@link Season} if type is not ALL
     * @return the stat value represented by this stat
     */
    Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period);

    //TODO override to do double and time formatting
    /**
     * Get the formatted stat value as a string
     * @param statContainer the statContainer to source the value from
     * @param type what type of period is being fetched from
     * @param period The period being fetched from, must be {@link Realm} or {@link Season} if type is not ALL
     * @return the formatted stat value
     */
    default String formattedStatValue(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        Long value = getStat(statContainer, type, period);
        if (value == null) {
            return "0";
        }
        return String.valueOf(value);
    }


    /**
     * Get the name that is stored in the DB
     * @return
     */
    @NotNull
    String getStatType();

    /**
     * Get the jsonb data in string format for this object
     * @return
     */
    @Nullable
    JSONObject getJsonData();

    //todo implement the following 2 functions for generic
    /**
     * Get the simple name of this stat, without qualifications (if present)
     *
     * i.e. Time Played, Flags Captured
     * @return the simple name
     */
    default String getSimpleName() {
        return getStatType();
    }

    /**
     * Get the qualified name of the stat, if one exists.
     * Should usually end with the {@link IStat#getSimpleName()}
     * <p>
     * i.e. Domination Time Played, Capture the Flag CTF_Oakvale Flags Captured
     * @return the qualified name
     */
    default String getQualifiedName() {
        return getSimpleName();
    }

    /**
     * Whether this stat is directly savable to the database
     * @return {@code true} if it is, {@code false} otherwise
     */
    boolean isSavable();

    /**
     * Whether this stat contains this otherSTat
     * @param otherStat
     * @return
     */
    boolean containsStat(IStat otherStat);

    /**
     * Gets the sum of all stats that meet the filter. A predicate that throws an {@link IllegalArgumentException} or {@link ClassCastException} will default to {@code false}
     * @param statContainer the statContainer to check
     * @param periodKey the periodKey of the stats
     * @param filter the predicate to check each stat against. If it throws an {@link IllegalArgumentException} or {@link ClassCastException}, that test is treated as false
     * @return the total value of the stats that meet the filter
     */
    default Long getFilteredStat(StatContainer statContainer, StatFilterType type, Period period, Predicate<Map.Entry<IStat, Long>> filter) {
        return statContainer.getStats().getStatsOfPeriod(type, period).entrySet().stream()
                .filter((entry) -> {
                    try {
                        return filter.test(entry);
                    } catch (IllegalArgumentException | ClassCastException ignored) {
                        return false;
                    }
                })
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /**
     * Whether this stat can be wrapped by an {@link IWrapperStat}
     * @return {@code true} if it can be wrapped, else {@code false}
     */
    default boolean wrappingAllowed() {
        return true;
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     * @return the generic stat
     */
    @NotNull
    IStat getGenericStat();
}
