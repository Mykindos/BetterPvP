package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatStatsRepository;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class GlobalCombatStatsRepository extends CombatStatsRepository<GlobalCombatData> {

    private final List<StatsRepository<?>> dependentRepositories = new ArrayList<>();

    @Inject
    protected GlobalCombatStatsRepository(Core plugin) {
        super(plugin);
    }

    public void addDependentRepository(StatsRepository<?> repository) {
        this.dependentRepositories.add(repository);
    }

    @Override
    protected void postSaveAll(boolean async) {
        dependentRepositories.forEach(repo -> repo.saveAll(async));
    }

    @Override
    public CompletableFuture<GlobalCombatData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final GlobalCombatData data = new GlobalCombatData(player);
            final UuidStatementValue uuid = new UuidStatementValue(player);
            Statement statement = new Statement("CALL GetCombatData(?)", uuid);
            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        data.setKills(result.getInt(1));
                        data.setDeaths(result.getInt(2));
                        data.setAssists(result.getInt(3));
                        data.setKillStreak(result.getInt(5));
                        data.setHighestKillStreak(result.getInt(6));

                        // We care if the rating is null because that means they have not played a game yet,
                        // so it falls back to the default value of rating in CombatData
                        // Contrary to the other stats, which are initialized to 0
                        int rating = result.getInt(4);
                        if (!result.wasNull()) {
                            data.setRating(rating);
                        }
                    }

                    attachmentLoaders.forEach(loader -> {
                        final ICombatDataAttachment attachment = loader.loadAttachment(player, data, database, plugin.getDatabasePrefix());
                        data.attach(attachment);
                    });
                } catch (SQLException e) {
                    log.error("Failed to load combat data for " + player, e);
                }
            });
            return data;
        }).exceptionally(throwable -> {
            log.error("Failed to load combat data for " + player, throwable);
            return null;
        });
    }

}
