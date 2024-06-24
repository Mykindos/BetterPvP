package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsFilter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
public class ChampionsStatsRepository extends StatsRepository<RoleStatistics> {

    private final RoleManager roleManager;

    @Inject
    public ChampionsStatsRepository(Champions champions, RoleManager roleManager, GlobalCombatStatsRepository globalRepo) {
        super(champions);
        this.roleManager = roleManager;
        globalRepo.addDependentRepository(this);
    }

    @Override
    public CompletableFuture<RoleStatistics> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final Map<ChampionsFilter, ChampionsCombatData> combatDataMap = new EnumMap<>(ChampionsFilter.class);
            final RoleStatistics roleStatistics = new RoleStatistics(combatDataMap, roleManager, player);
            final UuidStatementValue uuid = new UuidStatementValue(player);
            Statement statement = new Statement("CALL GetChampionsData(?)", uuid);
            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        final String className = result.getString(1);
                        final Role role = className.isEmpty() ? null : Role.valueOf(className);
                        ChampionsFilter filter = ChampionsFilter.fromRole(role);
                        final ChampionsCombatData data = new ChampionsCombatData(player, roleManager, role);
                        data.setKills(result.getInt(2));
                        data.setDeaths(result.getInt(3));
                        data.setAssists(result.getInt(4));
                        data.setKillStreak(result.getInt(6));
                        data.setHighestKillStreak(result.getInt(7));

                        // We care if the rating is null because that means they have not played a game yet,
                        // so it falls back to the default value of rating in CombatData
                        // Contrary to the other stats, which are initialized to 0
                        int rating = result.getInt(5);
                        if (!result.wasNull()) {
                            data.setRating(rating);
                        }

                        combatDataMap.put(filter, data);
                    }

                } catch (SQLException e) {
                    log.error("Failed to load combat data for " + player, e).submit();
                }
            });
            return roleStatistics;
        }).exceptionally(throwable -> {
            log.error("Failed to load combat data for " + player, throwable).submit();
            return null;
        });
    }

    public void validate(Client client, boolean isValid) {
        String updateKills = "UPDATE kills SET Valid = ? WHERE Killer = ?";
        Statement updateKillsStatement = new Statement(updateKills,
                new BooleanStatementValue(isValid),
                new UuidStatementValue(client.getUniqueId()));
        database.executeUpdate(updateKillsStatement);

        String updateStats = "UPDATE combat_stats SET Valid = ? WHERE Gamer = ?";
        Statement updateStatsStatement = new Statement(updateStats,
                new BooleanStatementValue(isValid),
                new UuidStatementValue(client.getUniqueId()));
        database.executeUpdate(updateStatsStatement);

        String updateCombatData = "UPDATE champions_combat_stats SET Valid = ? WHERE Gamer = ?";
        Statement updateCombatDataStatement = new Statement(updateCombatData,
                new BooleanStatementValue(isValid),
                new UuidStatementValue(client.getUniqueId()));
        database.executeUpdate(updateCombatDataStatement);
    }
}
