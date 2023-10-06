package me.mykindos.betterpvp.core.stats.sort;

import lombok.Getter;

/**
 * Sort types for leaderboards.
 */
public enum TemporalSort implements SortType {

    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    SEASONAL(999, "Seasonal");

    @Getter
    private final double days;

    @Getter
    private final String name;

    TemporalSort(double days, String name) {
        this.days = days;
        this.name = name;
    }
}
