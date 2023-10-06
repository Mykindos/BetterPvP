package me.mykindos.betterpvp.core.stats.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry<E, T> {

    private final E key;
    private T value;

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
