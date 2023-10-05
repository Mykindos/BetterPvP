package me.mykindos.betterpvp.core.utilities.model.leaderboard;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Sort types for leaderboards.
 */
public enum TemporalSort implements SortType {

    DAILY(1),
    WEEKLY(7),
    SEASONAL(999);

    @Getter
    private final double days;

    TemporalSort(double days) {
        this.days = days;
    }

    @NotNull
    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
