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
import me.mykindos.betterpvp.core.stats.SearchOptions;
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
public class MiningOresMinedLeaderboard extends Leaderboard<UUID, Long> {

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
    protected Comparator<Long> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(Long::intValue).reversed();
    }

    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        return repository.fetchDataAsync(entry).join().getOresMined();
    }

    @Override
    @SneakyThrows
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix) {
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
