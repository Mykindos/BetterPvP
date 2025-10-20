package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.client.stats.StatContainer;

import java.util.Map;
import java.util.function.Predicate;

public interface IStat {
    /**
     * Get the stat represented by this object from the statContainer
     * @param statContainer the statContainer to source the value from
     * @param periodKey the period to fetch from
     * @return the stat value represented by this stat
     */
    Double getStat(StatContainer statContainer, String periodKey);

    /**
     * Get the name that is stored in the DB
     * @return
     */
    String getStatName();

    /**
     * Whether this stat is directly savable to the database
     * @return {@code true} if it is, {@code false} otherwise
     */
    boolean isSavable();

    /**
     * Whether this stat contains this statName
     * @param statName
     * @return
     */
    @Deprecated
    boolean containsStat(String statName);
    /**
     * Whether this stat contains this otherSTat
     * @param otherStat
     * @return
     */
    default boolean containsStat(IStat otherStat) {
        return containsStat(otherStat.getStatName());
    }

    /**
     * Gets the sum of all stats that meet the filter. A predicate that throws an {@link IllegalArgumentException} or {@link ClassCastException} will default to {@code false}
     * @param statContainer the statContainer to check
     * @param periodKey the periodKey of the stats
     * @param filter the predicate to check each stat against. If it throws an {@link IllegalArgumentException} or {@link ClassCastException}, that test is treated as false
     * @return the total value of the stats that meet the filter
     */
    default Double getFilteredStat(StatContainer statContainer, String periodKey, Predicate<Map.Entry<IStat, Double>> filter) {
        return statContainer.getStats().getStatsOfPeriod(periodKey).entrySet().stream()
                .filter((entry) -> {
                    try {
                        return filter.test(entry);
                    } catch (IllegalArgumentException | ClassCastException ignored) {
                        return false;
                    }
                })
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    /**
     * Whether this stat can be wrapped by an {@link IWrapperStat}
     * @return {@code true} if it can be wrapped, else {@code false}
     */
    default boolean wrappingAllowed() {
        return true;
    }


}
