package me.mykindos.betterpvp.core.stats.repository;
import java.util.Comparator;

public class LeaderboardEntryComparator<E, T extends Comparable<T>> implements Comparator<LeaderboardEntry<E, T>> {
    @Override
    public int compare(LeaderboardEntry<E, T> entry1, LeaderboardEntry<E, T> entry2) {
        // Compare the value fields first
        int valueComparison = entry2.getValue().compareTo(entry1.getValue());

        // If the values are equal, compare the key fields
        if (valueComparison == 0) {
            return entry1.getKey().toString().compareTo(entry2.getKey().toString());
        }

        return valueComparison; // Return the result of comparing values
    }
}