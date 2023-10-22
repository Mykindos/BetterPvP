package me.mykindos.betterpvp.core.combat.stats;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CombatStatsRepository extends StatsRepository<CombatData> {

    private final List<AttachmentLoader> attachmentLoaders = new ArrayList<>();

    @Inject
    protected CombatStatsRepository(Core plugin) {
        super(plugin);
    }

    public void addAttachmentLoader(AttachmentLoader attachmentLoader) {
        this.attachmentLoaders.add(attachmentLoader);
    }

    @Override
    public CompletableFuture<CombatData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final CombatData data = new CombatData(player);
            final UuidStatementValue uuid = new UuidStatementValue(player);
            Statement statement = new Statement("CALL GetCombatData(?)", uuid);
            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        data.setKills(result.getInt("Kills"));
                        data.setDeaths(result.getInt("Deaths"));
                        data.setAssists(result.getInt("Assists"));
                        data.setKillStreak(result.getInt("KillStreak"));
                        data.setHighestKillStreak(result.getInt("HighestKillStreak"));
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
