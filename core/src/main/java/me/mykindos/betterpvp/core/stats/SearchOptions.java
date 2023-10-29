package me.mykindos.betterpvp.core.stats;

import lombok.Builder;
import lombok.Value;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import org.jetbrains.annotations.Nullable;

@Value
@Builder
public class SearchOptions {

    @Nullable
    SortType sort;

    @Nullable
    FilterType filter;

    public static SearchOptions EMPTY = SearchOptions.builder().build();

    public <E> boolean accepts(E value) {
        return (sort == null || sort.accepts(value)) && (filter == null || filter.accepts(value));
    }
}
