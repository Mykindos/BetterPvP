package me.mykindos.betterpvp.core.stats.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class LeaderboardEntryKey<T> {

    private final @NotNull SortType sortType;
    private final @NotNull T value;

    public static <T> LeaderboardEntryKey<T> of(@NotNull SortType sortType, @NotNull T key) {
        return new LeaderboardEntryKey<>(sortType, key);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeaderboardEntryKey && ((LeaderboardEntryKey<?>) obj).value.equals(value) && ((LeaderboardEntryKey<?>) obj).sortType.equals(sortType);
    }

    @Override
    public int hashCode() {
        return value.hashCode() + sortType.hashCode();
    }
}
