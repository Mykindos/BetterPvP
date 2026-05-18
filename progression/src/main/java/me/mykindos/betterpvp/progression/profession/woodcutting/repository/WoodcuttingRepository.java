package me.mykindos.betterpvp.progression.profession.woodcutting.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.ProgressionWoodcuttingRecord;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_WOODCUTTING;

@CustomLog
@Singleton
public class WoodcuttingRepository {

    private final Database database;
    private final ClientManager clientManager;
    private final ProfessionProfileManager profileManager;
    private final Queue<ProgressionWoodcuttingRecord> queuedChoppedLogs = new ConcurrentLinkedQueue<>();

    @Inject
    public WoodcuttingRepository(Database database, ClientManager clientManager, ProfessionProfileManager profileManager, Progression progression) {
        this.database = database;
        this.clientManager = clientManager;
        this.profileManager = profileManager;
        createPartitions();

        UtilServer.runTaskTimer(progression, () -> processQueuedChoppedLogs(true), 600L, 600L);
    }

    public void processQueuedChoppedLogs(boolean async) {
        if (queuedChoppedLogs.isEmpty()) {
            return;
        }

        List<ProgressionWoodcuttingRecord> logsToSave = new ArrayList<>();
        while (!queuedChoppedLogs.isEmpty() && logsToSave.size() < 500) {
            logsToSave.add(queuedChoppedLogs.poll());
        }

        if (async) {
            database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.batchInsert(logsToSave).execute();
                log.info("Saved {} chopped logs asynchronously", logsToSave.size());
            }).exceptionally(ex -> {
                log.error("Failed to batch insert chopped logs", ex).submit();
                return null;
            });
        } else {
            database.getDslContext().batchInsert(logsToSave).execute();
        }

    }

    public void createPartitions() {
        int season = Core.getCurrentRealm().getSeason().getId();
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

    public void saveChoppedLog(Player player, Material material, Location location, int amount) {
        Client client = clientManager.search().online(player);
        if (client == null) return;

        ProgressionWoodcuttingRecord record = new ProgressionWoodcuttingRecord();
        record.setClient(client.getId());
        record.setSeason(Core.getCurrentRealm().getSeason().getId());
        record.setMaterial(material.name());
        record.setLocation(UtilWorld.locationToString(location));
        record.setAmount(amount);
        record.setTimestamp(System.currentTimeMillis());

        queuedChoppedLogs.add(record);
    }

    /**
     * Gets the total chopped logs for a player given a unique ID
     */
    public Long getTotalChoppedLogsForPlayer(UUID playerUUID) {
        return database.getAsyncDslContext().executeAsync(ctx ->
                ctx.select(DSL.coalesce(DSL.sum(PROGRESSION_WOODCUTTING.AMOUNT), 0L))
                        .from(PROGRESSION_WOODCUTTING)
                        .join(CLIENTS).on(PROGRESSION_WOODCUTTING.CLIENT.eq(CLIENTS.ID))
                        .where(CLIENTS.UUID.eq(playerUUID.toString()))
                        .fetchOne(0, Long.class)
        ).join();
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
        }).exceptionally(ex -> {
            log.error("Error fetching woodcutting leaderboard data", ex).submit();
            return new HashMap<>();
        });
    }

}
