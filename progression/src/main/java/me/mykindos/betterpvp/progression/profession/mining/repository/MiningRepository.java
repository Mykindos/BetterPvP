package me.mykindos.betterpvp.progression.profession.mining.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.impl.DSL;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_PROPERTIES;

@CustomLog
@Singleton
public class MiningRepository {

    private final Database database;

    @Inject
    public MiningRepository(Database database) {
        this.database = database;
        createPartitions();
    }

    public void createPartitions() {
        int season = Core.getCurrentRealm().getSeason();
        String partitionTableName = "progression_mining_season_" + season;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF progression_mining FOR VALUES IN (%d)",
                    partitionTableName, season
            )));
            log.info("Created partition {} for season {}", partitionTableName, season).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }
    public CompletableFuture<Integer> getOresMinedForGamer(UUID player) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            try {
                String totalOresMined = ctx.select(PROGRESSION_PROPERTIES.VALUE).from(PROGRESSION_PROPERTIES)
                        .where(PROGRESSION_PROPERTIES.CLIENT.eq(ctx.select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(player.toString()))))
                        .and(PROGRESSION_PROPERTIES.SEASON.eq(Core.getCurrentRealm().getSeason()))
                        .and(PROGRESSION_PROPERTIES.PROPERTY.eq("TOTAL_ORES_MINED")).fetchOne(PROGRESSION_PROPERTIES.VALUE);
                if (totalOresMined == null) return 0;

                return Integer.parseInt(totalOresMined);
            } catch (Exception ex) {
                log.error("Failed to load mining data for {}", player.toString(), ex).submit();
            }

            return 0;
        });

    }

}
