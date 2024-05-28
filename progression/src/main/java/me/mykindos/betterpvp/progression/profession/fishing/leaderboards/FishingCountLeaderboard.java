package me.mykindos.betterpvp.progression.profession.fishing.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.repository.FishingRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@CustomLog
@Singleton
public class FishingCountLeaderboard extends PlayerLeaderboard<Long> implements Sorted {

    private final FishingRepository fishingRepository;

    @Inject
    public FishingCountLeaderboard(Progression progression, FishingRepository fishingRepository) {
        super(progression);
        this.fishingRepository = fishingRepository;
        init();
    }

    @Override
    public String getName() {
        return "Total Fish Caught";
    }

    @Override
    public Comparator<Long> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(Long::intValue).reversed();
    }

    @Override
    public SortType [] acceptedSortTypes() {
        return TemporalSort.values();
    }

    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    @SneakyThrows
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        return fishingRepository.getCaughtCount(entry);
    }

    @Override
    @SneakyThrows
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        return fishingRepository.getTopFishCaughtCount(type.getDays()).join();
    }
}
