package me.mykindos.betterpvp.progression.tree.mining.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.Viewable;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.mining.repository.MiningRepository;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class MiningOresMinedLeaderboard extends Leaderboard<UUID, Long> implements Viewable {

    private final MiningRepository repository;

    @Inject
    public MiningOresMinedLeaderboard(Progression progression, MiningRepository repository) {
        super(progression);
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Total Ores Mined";
    }

    @Override
    protected Comparator<Long> getSorter(SortType sortType) {
        return Comparator.comparing(Long::intValue).reversed();
    }

    @Override
    public SortType[] acceptedSortTypes() {
        return new SortType[]{TemporalSort.SEASONAL};
    }

    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    protected Long fetch(SortType sortType, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        if (sortType != TemporalSort.SEASONAL) {
            log.error("Attempted to fetch leaderboard data for " + entry + " with invalid sort type " + sortType);
            return 0L;
        }

        return repository.fetchDataAsync(entry).join().getOresMined();
    }

    @SneakyThrows
    @Override
    protected Map<UUID, Long> fetchAll(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix) {
        if (sortType != TemporalSort.SEASONAL) {
            log.error("Attempted to fetch leaderboard data for all with invalid sort type " + sortType);
            return new HashMap<>();
        }

        Map<UUID, Long> leaderboard = new HashMap<>();
        Statement statement = new Statement("CALL GetTopMiningByOre(?, ?, ?)",
                new IntegerStatementValue(10),
                new StringStatementValue(repository.getDbMaterialsList()),
                new StringStatementValue(tablePrefix));
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
