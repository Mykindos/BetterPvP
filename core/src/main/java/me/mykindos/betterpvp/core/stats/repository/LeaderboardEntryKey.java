package me.mykindos.betterpvp.core.stats.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class LeaderboardEntryKey<T> {

    private final @NotNull SearchOptions options;
    private final @NotNull T value;

    public static <T> LeaderboardEntryKey<T> of(@NotNull SearchOptions options, @NotNull T key) {
        return new LeaderboardEntryKey<>(options, key);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeaderboardEntryKey && ((LeaderboardEntryKey<?>) obj).value.equals(value) && ((LeaderboardEntryKey<?>) obj).options.equals(options);
    }

    @Override
    public int hashCode() {
        return value.hashCode() + options.hashCode();
    }
}
