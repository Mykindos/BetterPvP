package me.mykindos.betterpvp.progression.profession.woodcutting.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_WOODCUTTING;

@CustomLog
@Singleton
public class WoodcuttingRepository {

    private final Database database;
    private final ClientManager clientManager;
    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingRepository(Database database, ClientManager clientManager, ProfessionProfileManager profileManager) {
        this.database = database;
        this.clientManager = clientManager;
        this.profileManager = profileManager;
        createPartitions();
    }

    public void createPartitions() {
        int season = Core.getCurrentSeason();
        String partitionTableName = "progression_woodcutting_season_" + season;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF progression_woodcutting FOR VALUES IN (%d)",
                    partitionTableName, season
            )));
            log.info("Created partition {} for season {}", partitionTableName, season).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }

    public void saveChoppedLog(UUID playerUUID, Material material, Location location, int amount) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Client client = clientManager.search().offline(playerUUID).join().orElseThrow();

            ctx.insertInto(PROGRESSION_WOODCUTTING)
                    .set(PROGRESSION_WOODCUTTING.CLIENT, client.getId())
                    .set(PROGRESSION_WOODCUTTING.SEASON, Core.getCurrentSeason())
                    .set(PROGRESSION_WOODCUTTING.MATERIAL, material.name())
                    .set(PROGRESSION_WOODCUTTING.LOCATION, UtilWorld.locationToString(location))
                    .set(PROGRESSION_WOODCUTTING.AMOUNT, amount)
                    .execute();
        });

    }

    /**
     * Gets the total chopped logs for a player given a unique ID
     */
    public Long getTotalChoppedLogsForPlayer(UUID playerUUID) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            Client client = clientManager.search().offline(playerUUID).join().orElse(null);

            if (client == null) {
                return 0L;
            }

            Long totalAmount = ctx.select(DSL.coalesce(DSL.sum(PROGRESSION_WOODCUTTING.AMOUNT), 0L))
                    .from(PROGRESSION_WOODCUTTING)
                    .where(PROGRESSION_WOODCUTTING.CLIENT.eq(client.getId()))
                    .fetchOne(0, Long.class);

            return totalAmount != null ? totalAmount : 0L;
        }).join();
    }

    /**
     * docs tbd
     */
    public CompletableFuture<HashMap<UUID, Long>> getTopLogsChoppedByCount(double days) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            try {

                ctx.select(CLIENTS.UUID, DSL.sum(PROGRESSION_WOODCUTTING.AMOUNT).as("total_amount"))
                        .from(PROGRESSION_WOODCUTTING)
                        .join(CLIENTS).on(PROGRESSION_WOODCUTTING.CLIENT.eq(CLIENTS.ID))
                        .where(PROGRESSION_WOODCUTTING.TIMESTAMP.gt(Instant.now().minus(Duration.ofDays((int) days)).toEpochMilli()))
                        .groupBy(CLIENTS.UUID)
                        .orderBy(DSL.sum(PROGRESSION_WOODCUTTING.AMOUNT).desc())
                        .limit(10)
                        .fetch()
                        .forEach(record -> {
                            leaderboard.put(
                                    UUID.fromString(record.get(CLIENTS.UUID)),
                                    record.get("total_amount", Long.class)
                            );
                        });
            } catch (DataAccessException ex) {
                log.error("Error fetching woodcutting leaderboard data", ex).submit();
            }
            return leaderboard;
        });
    }

}
