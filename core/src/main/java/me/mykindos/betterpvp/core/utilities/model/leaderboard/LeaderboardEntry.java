package me.mykindos.betterpvp.core.utilities.model.leaderboard;

import lombok.Data;

@Data
public class LeaderboardEntry<E, T> {

    private final E key;
    private final T value;

    public static <E, T> LeaderboardEntry<E, T> of(E entryName, T value) {
        return new LeaderboardEntry<>(entryName, value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeaderboardEntry && ((LeaderboardEntry<?, ?>) obj).key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
