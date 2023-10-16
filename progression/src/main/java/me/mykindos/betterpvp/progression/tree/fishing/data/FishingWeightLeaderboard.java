package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Singleton
public class FishingWeightLeaderboard extends Leaderboard<UUID, Long> {

    @Inject
    public FishingWeightLeaderboard(Progression progression) {
        super(progression, progression.getDatabasePrefix());
    }

    @Override
    public String getName() {
        return "Total Weight Caught";
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
    protected Long fetch(SortType sortType, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        AtomicLong weight = new AtomicLong();
        final TemporalSort type = (TemporalSort) sortType;
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
    protected Map<UUID, Long> fetchAll(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix) {
        Map<UUID, Long> leaderboard = new HashMap<>();

        final TemporalSort type = (TemporalSort) sortType;
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
