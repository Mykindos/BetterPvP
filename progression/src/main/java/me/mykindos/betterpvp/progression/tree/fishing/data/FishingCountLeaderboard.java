package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.Leaderboard;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.SortType;
import me.mykindos.betterpvp.core.utilities.model.leaderboard.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class FishingCountLeaderboard extends Leaderboard<UUID, Long> {

    @Inject
    public FishingCountLeaderboard(Progression progression) {
        super(progression, progression.getDatabasePrefix());
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
    protected SortType[] acceptedSortTypes() {
        return TemporalSort.values();
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
