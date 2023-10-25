package me.mykindos.betterpvp.core.stats.repository;
import java.util.Comparator;

public class LeaderboardEntryComparator<E, T> implements Comparator<LeaderboardEntry<E, T>> {

    private final Comparator<T> comparator;

    public LeaderboardEntryComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int compare(LeaderboardEntry<E, T> entry1, LeaderboardEntry<E, T> entry2) {
        // Compare the value fields first
        int valueComparison = comparator.compare(entry2.getValue(), entry1.getValue());

        // If the values are equal, compare the key fields
        if (valueComparison == 0) {
            return entry1.getKey().toString().compareTo(entry2.getKey().toString());
        }

        return valueComparison; // Return the result of comparing values
    }
}