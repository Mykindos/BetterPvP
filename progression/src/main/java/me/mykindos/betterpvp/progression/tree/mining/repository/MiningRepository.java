package me.mykindos.betterpvp.progression.tree.mining.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.stats.ProgressionStatsRepository;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import me.mykindos.betterpvp.progression.tree.mining.MiningService;
import me.mykindos.betterpvp.progression.tree.mining.data.MiningData;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class MiningRepository extends ProgressionStatsRepository<Mining, MiningData> {

    private final MiningService service;

    @Inject
    public MiningRepository(Progression progression, MiningService service) {
        super(progression, "Mining");
        this.service = service;
    }

    public String getDbMaterialsList() {
        return service.getLeaderboardBlocks().stream()
                .map(mat -> "'" + mat.name() + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("''");
    }

    @Override
    public CompletableFuture<MiningData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final MiningData data = new MiningData();
            Statement statement = new Statement("CALL GetGamerOresMined(?, ?, ?)",
                    new UuidStatementValue(player),
                    new StringStatementValue(getDbMaterialsList()),
                    new StringStatementValue(plugin.getDatabasePrefix()));
            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        data.setOresMined(result.getLong(1));
                    }
                } catch (SQLException e) {
                    log.error("Failed to load mining data for " + player, e);
                }
            });
            return data;
        }).exceptionally(throwable -> {
            log.error("Failed to load mining data for " + player, throwable);
            return null;
        });
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        // ignore
    }
}
