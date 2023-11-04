package me.mykindos.betterpvp.core.stats.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LeaderboardEntry<E, T> {

    private final E key;
    private T value;

    public static <E, T> LeaderboardEntry<E, T> of(E entryName, T value) {
        return new LeaderboardEntry<>(entryName, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if(!(obj instanceof LeaderboardEntry<?, ?> other)) {
            return false;
        }

        return this.key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key.toString() + ": " + value.toString();
    }
}
