package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FishingCountLeaderboard extends Leaderboard<UUID, Long> {

    private final FishingRepository repository;

    @Inject
    public FishingCountLeaderboard(Progression progression, FishingRepository repository) {
        super(progression, progression.getDatabasePrefix());
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Total Fish Caught";
    }

    @Override
    protected Comparator<Long> getSorter() {
        return Comparator.comparing(Long::intValue).reversed();
    }

    @Override
    public SortType[] acceptedSortTypes() {
        return TemporalSort.values();
    }

    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    protected CompletableFuture<Long> loadEntryData(SortType type, UUID entry) {
        // todo get entry properly from database based on sort type
        return repository.getDataAsync(entry).thenApply(FishingData::getFishCaught);
    }

    @SneakyThrows
    @Override
    protected Map<UUID, Long> fetch(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix) {
        Map<UUID, Long> leaderboard = new HashMap<>();

        final TemporalSort type = (TemporalSort) sortType;
        Statement statement = new Statement("CALL GetTopFishingByCount(?, ?)",
                new DoubleStatementValue(type.getDays()),
                new IntegerStatementValue(10)); // Top 10
        database.executeProcedure(statement, -1, result -> {
            try {
                while (result.next()) {
                    final String gamer = result.getString(1);
                    final long count = result.getLong(2);
                    leaderboard.put(UUID.fromString(gamer), count);
                }
            } catch (SQLException e) {
                log.error("Error fetching leaderboard data", e);
            }
        });

        return leaderboard;
    }
}
