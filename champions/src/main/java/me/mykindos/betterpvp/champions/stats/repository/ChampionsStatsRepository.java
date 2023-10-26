package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@Slf4j
public class ChampionsStatsRepository extends StatsRepository<RoleStatistics> {

    @Inject
    public ChampionsStatsRepository(Champions champions) {
        super(champions);
    }

    @Override
    public CompletableFuture<RoleStatistics> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final Map<Role, ChampionsCombatData> combatDataMap = new HashMap<>();
            final RoleStatistics data = new RoleStatistics(combatDataMap);
            final UuidStatementValue uuid = new UuidStatementValue(player);
            Statement statement = new Statement("CALL GetChampionsData(?)", uuid);
            database.executeProcedure(statement, -1, result -> {
//                try {
//                    if (result.next()) {
//                        data.setKills(result.getInt(1));
//                        data.setDeaths(result.getInt(2));
//                        data.setAssists(result.getInt(3));
//                        data.setKillStreak(result.getInt(5));
//                        data.setHighestKillStreak(result.getInt(6));
//
//                        // We care if the rating is null because that means they have not played a game yet,
//                        // so it falls back to the default value of rating in CombatData
//                        // Contrary to the other stats, which are initialized to 0
//                        int rating = result.getInt(4);
//                        if (!result.wasNull()) {
//                            data.setRating(rating);
//                        }
//                    }
//
//                } catch (SQLException e) {
//                    log.error("Failed to load combat data for " + player, e);
//                }
            });
            return data;
        }).exceptionally(throwable -> {
            log.error("Failed to load combat data for " + player, throwable);
            return null;
        });
    }
}
