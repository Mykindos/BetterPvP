package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.CombatSort;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.Viewable;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
@Slf4j
public class GlobalCombatLeaderboard extends Leaderboard<UUID, CombatData> implements Viewable {

    private static final Map<SortType, Statement> TOP_SORT_STATEMENTS = ImmutableMap.of(
            CombatSort.RATING, new Statement("CALL GetTopRating(?)", new IntegerStatementValue(10)),
            CombatSort.KILLS, new Statement("CALL GetTopKills(?)", new IntegerStatementValue(10)),
            CombatSort.DEATHS, new Statement("CALL GetTopDeaths(?)", new IntegerStatementValue(10)),
            CombatSort.KDR, new Statement("CALL GetTopKDR(?)", new IntegerStatementValue(10)),
            CombatSort.KILLSTREAK, new Statement("CALL GetTopKillstreak(?)", new IntegerStatementValue(10)),
            CombatSort.HIGHEST_KILLSTREAK, new Statement("CALL GetTopHighestKillstreak(?)", new IntegerStatementValue(10))
    );

    private final GlobalCombatStatsRepository repository;

    @Inject
    protected GlobalCombatLeaderboard(Core core, GlobalCombatStatsRepository repository) {
        super(core);
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Combat";
    }

    @Override
    protected Comparator<CombatData> getSorter(SortType sortType) {
        return Comparator.comparing((Function<CombatData, Float>) (data -> switch ((CombatSort) sortType) {
            case RATING -> (float) data.getRating();
            case KILLS -> (float) data.getKills();
            case DEATHS -> (float) data.getDeaths();
            case KDR -> data.getKillDeathRatio();
            case KILLSTREAK -> (float) data.getKillStreak();
            case HIGHEST_KILLSTREAK -> (float) data.getHighestKillStreak();
        })).reversed();
    }

    @Override
    public SortType[] acceptedSortTypes() {
        return CombatSort.values();
    }

    @Override
    public CompletableFuture<Map<SortType, Integer>> add(@NotNull UUID entryName, @NotNull CombatData add) {
        throw new UnsupportedOperationException("Cannot add combat data, only compute/replace");
    }

    @Override
    protected CombatData join(CombatData value, CombatData add) {
        throw new UnsupportedOperationException("Cannot join combat data, only compute/replace");
    }

    @Override
    protected CombatData fetch(SortType sortType, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        // We can join this because fetch is run on a separate thread
        return repository.getDataAsync(entry).join();
    }

    @Override
    protected Map<UUID, CombatData> fetchAll(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix) {
        Map<UUID, CombatData> map = new HashMap<>();
        Statement stmt = TOP_SORT_STATEMENTS.get(sortType);
        database.executeProcedure(stmt, -1, result -> {
            try {
                while (result.next()) {
                    final UUID gamer = UUID.fromString(result.getString("Gamer"));
                    // We can join this because fetchAll is run on a separate thread
                    final GlobalCombatData data = repository.getDataAsync(gamer).join();
                    map.put(gamer, data);
                }
            } catch (SQLException e) {
                log.error("Failed to load combat rating leaderboard for type " + sortType, e);
            }
        });
        return map;
    }

}
