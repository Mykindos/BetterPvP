package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@CustomLog
@Singleton
public class FishingWeightLeaderboard extends PlayerLeaderboard<Long> implements Sorted {

    @Inject
    public FishingWeightLeaderboard(Progression progression) {
        super(progression);
        init();
    }

    @Override
    public String getName() {
        return "Total Weight Caught";
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
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        AtomicLong weight = new AtomicLong();
        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        Statement statement = new Statement("CALL GetGamerFishingWeight(?, ?)",
                new UuidStatementValue(entry),
                new DoubleStatementValue(type.getDays())); // Top 10
        database.executeProcedure(statement, -1, result -> {
            try {
                if (result.next()) {
                    weight.set(result.getLong(1));
                }
            } catch (SQLException e) {
                log.error("Error fetching leaderboard data", e);
            }
        });

        return weight.get();
    }

    @SneakyThrows
    @Override
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        Map<UUID, Long> leaderboard = new HashMap<>();

        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        Statement statement = new Statement("CALL GetTopFishingByWeight(?, ?)",
                new DoubleStatementValue(type.getDays()),
                new IntegerStatementValue(10)); // Top 10
        database.executeProcedure(statement, -1, result -> {
            try {
                while (result.next()) {
                    final String gamer = result.getString(1);
                    final long weight = result.getLong(2);
                    leaderboard.put(UUID.fromString(gamer), weight);
                }
            } catch (SQLException e) {
                log.error("Error fetching leaderboard data", e);
            }
        });

        return leaderboard;
    }
}
