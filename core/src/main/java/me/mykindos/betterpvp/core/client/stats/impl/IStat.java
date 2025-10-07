package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.client.stats.StatContainer;

import java.util.Map;
import java.util.function.Predicate;

public interface IStat {
    /**
     * Get the stat represented by this object from the statContainer
     * @param statContainer
     * @param periodKey
     * @return
     */
    Double getStat(StatContainer statContainer, String periodKey);

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
    boolean containsStat(String statName);

    /**
     * Gets the sum of all stats that meet the filter. A predicate that throws an {@link IllegalArgumentException} will default to {@code false}
     * @param statContainer the statContainer to check
     * @param periodKey the periodKey of the stats
     * @param filter the predicate to check each stat against. If it throws an {@link IllegalArgumentException}, that test is treated as false
     * @return the total value of the stats that meet the filter
     */
    default Double getFilteredStat(StatContainer statContainer, String periodKey, Predicate<Map.Entry<String, Double>> filter) {
        return statContainer.getStats().getStatsOfPeriod(periodKey).entrySet().stream()
                .filter((entry) -> {
                    try {
                        return filter.test(entry);
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }


}
