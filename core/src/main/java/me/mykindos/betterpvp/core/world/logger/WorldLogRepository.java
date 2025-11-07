package me.mykindos.betterpvp.core.world.logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.GetWorldLogsForBlock;
import me.mykindos.betterpvp.core.database.jooq.tables.records.WorldLogsMetadataRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.WorldLogsRecord;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_WORLD_LOGS_FOR_BLOCK;
import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS_METADATA;

/**
 * Repository responsible for managing and persisting world block change logs.
 * Handles saving, retrieving, and processing block modifications across servers.
 * Uses a transactional approach to ensure data consistency when saving multiple logs.
 */
@Singleton
@CustomLog
public class WorldLogRepository {

    private final Database database;
    private static final Gson GSON = new Gson();
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    @Inject
    public WorldLogRepository(Database database) {
        this.database = database;

        createPartitions();

        UtilServer.runTaskTimer(JavaPlugin.getPlugin(Core.class), () -> {
            purgeLogs(7);
        }, 100L, 20 * 60 * 60L);
    }

    public void createPartitions() {
        int realm = Core.getCurrentRealm();
        String partitionTableName = "world_logs_realm_" + realm;
        String metadataPartitionTableName = "world_logs_metadata_realm_" + realm;

        try {
            database.getDslContext().transaction(config -> {
                DSLContext ctx = DSL.using(config);

                // Create partition for world_logs if it doesn't exist
                try {
                    ctx.execute(DSL.sql(String.format(
                            "CREATE TABLE IF NOT EXISTS %s PARTITION OF world_logs FOR VALUES IN (%d)",
                            partitionTableName, realm
                    )));
                    log.info("Created partition {} for realm {}", partitionTableName, realm).submit();
                } catch (Exception e) {
                    log.info("Partition {} may already exist", partitionTableName).submit();
                }

                // Create partition for world_logs_metadata if it doesn't exist
                try {
                    ctx.execute(DSL.sql(String.format(
                            "CREATE TABLE IF NOT EXISTS %s PARTITION OF world_logs_metadata FOR VALUES IN (%d)",
                            metadataPartitionTableName, realm
                    )));
                    log.info("Created partition {} for realm {}", metadataPartitionTableName, realm).submit();
                } catch (Exception e) {
                    log.info("Partition {} may already exist", metadataPartitionTableName).submit();
                }
            });
        } catch (Exception ex) {
            log.error("Error creating partitions for realm {}", realm, ex).submit();
        }
    }

    /**
     * Saves a list of world logs to the database using a transaction.
     * Creates both main log entries and associated metadata entries.
     * All logs are saved with a unique UUID to link metadata.
     *
     * @param logs List of WorldLog objects to save
     */
    public void saveLogs(List<WorldLog> logs) {
        if (logs.isEmpty()) {
            return;
        }


        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            try {
                ctx.transaction(config -> {
                    DSLContext ctxl = DSL.using(config);

                    // Prepare log records
                    List<WorldLogsRecord> logRecords = new ArrayList<>();
                    List<WorldLogsMetadataRecord> metadataRecords = new ArrayList<>();

                    for (WorldLog log : logs) {
                        long id = ID_GENERATOR.nextId();

                        // Create main log record
                        WorldLogsRecord logRecord = ctxl.newRecord(WORLD_LOGS);
                        logRecord.setId(id);
                        logRecord.setRealm(Core.getCurrentRealm());
                        logRecord.setWorld(log.getWorld());
                        logRecord.setBlockX(log.getBlockX());
                        logRecord.setBlockY(log.getBlockY());
                        logRecord.setBlockZ(log.getBlockZ());
                        logRecord.setAction(log.getAction());
                        logRecord.setMaterial(log.getMaterial());
                        logRecord.setBlockData(log.getBlockData() != null ? log.getBlockData().getAsString() : null);

                        // Handle ItemStack serialization
                        if (log.getItemStack() != null && !log.getItemStack().getType().isAir()) {
                            logRecord.setItemStack(log.getItemStack().serializeAsBytes());
                        }

                        logRecord.setTime(log.getTime().toEpochMilli());
                        logRecords.add(logRecord);

                        // Create metadata records
                        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
                            log.getMetadata().forEach((key, value) -> {
                                WorldLogsMetadataRecord metadataRecord = ctxl.newRecord(WORLD_LOGS_METADATA);
                                metadataRecord.setLogId(id);
                                metadataRecord.setRealm(Core.getCurrentRealm());
                                metadataRecord.setMetaKey(key);
                                metadataRecord.setMetaValue(value);
                                metadataRecords.add(metadataRecord);
                            });
                        }
                    }

                    // Batch insert logs
                    if (!logRecords.isEmpty()) {
                        ctxl.batchInsert(logRecords).execute();
                        log.info("Inserted {} world log records", logRecords.size()).submit();
                    }

                    // Batch insert metadata
                    if (!metadataRecords.isEmpty()) {
                        ctxl.batchInsert(metadataRecords).execute();
                        log.info("Inserted {} metadata records", metadataRecords.size()).submit();
                    }
                });
            } catch (Exception ex) {
                log.error("Failed to save world logs", ex).submit();
            }
        });

    }

    /**
     * Processes a world log session by executing the stored query and populating results.
     * Handles pagination and calculates total pages available.
     * Deserializes stored metadata and item stacks from the database.
     *
     * @param session The session containing the query and storage for results
     */
    public void processSession(WorldLogSession session, int page) {

        session.setData(new ArrayList<>());


        try {
            Block block = session.getBlock();
            var results = GET_WORLD_LOGS_FOR_BLOCK(database.getDslContext().configuration(),
                    Core.getCurrentRealm(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), (page - 1) * 10, 10);

            for (var logRecord : results) {
                String world = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.WORLD);
                int x = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.BLOCK_X);
                int y = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.BLOCK_Y);
                int z = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.BLOCK_Z);
                String action = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.ACTION);
                String material = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.MATERIAL);
                String blockData = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.BLOCK_DATA);
                byte[] itemStack = logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.ITEMSTACK);
                Instant time = Instant.ofEpochMilli(logRecord.get(GET_WORLD_LOGS_FOR_BLOCK.TIME_VAL));
                String metadataJson = logRecord.get(GetWorldLogsForBlock.GET_WORLD_LOGS_FOR_BLOCK.METADATA).data();
                long count = logRecord.get(GetWorldLogsForBlock.GET_WORLD_LOGS_FOR_BLOCK.TOTAL);

                session.setPages((int) Math.ceil(count / 10.0) - 1);

                HashMap<String, String> metadata = new HashMap<>();
                // Parse metadata
                if (metadataJson != null) {
                    List<MetadataEntry> metadataEntries = GSON.fromJson(metadataJson, new TypeToken<List<MetadataEntry>>() {
                    }.getType());
                    metadata = new HashMap<>();
                    for (MetadataEntry entry : metadataEntries) {
                        metadata.put(entry.getKey(), entry.getValue());
                    }
                }


                WorldLog log = WorldLog.builder()
                        .world(world)
                        .blockX(x).blockY(y).blockZ(z)
                        .action(WorldLogAction.valueOf(action))
                        .material(material)
                        .metadata(metadata)
                        .time(time)
                        .build();

                if (itemStack != null) {
                    log.setItemStack(ItemStack.deserializeBytes(itemStack));
                }

                session.getData().add(log);
            }
        } catch (Exception e) {
            log.error("Error processing session", e).submit();
        }


    }

    /**
     * Creates a database query statement to retrieve logs for a specific block.
     * Filters by server, world, and block coordinates.
     * Orders results by time descending and limits to 10 entries per page.
     *
     * @param block The block to query logs for
     * @return Statement object ready for execution
     */
    public GetWorldLogsForBlock getStatementForBlock(Block block) {
        return GET_WORLD_LOGS_FOR_BLOCK(Core.getCurrentRealm(),
                block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(),
                10, 0);
    }

    public void purgeLogs(int days) {
        Instant cutoff = Instant.now().minusSeconds(days * 24L * 60L * 60L);

        CompletableFuture.runAsync(() -> {
            try {
                DSLContext ctx = database.getDslContext();

                int deletedRows = ctx.deleteFrom(WORLD_LOGS)
                        .where(WORLD_LOGS.REALM.eq(Core.getCurrentRealm()))
                        .and(WORLD_LOGS.TIME.le(cutoff.toEpochMilli()))
                        .limit(50000)
                        .execute();

                log.info("Finished purging world_logs, deleted {} rows", deletedRows).submit();
            } catch (Exception ex) {
                log.error("Failed to purge world_logs", ex).submit();
            }
        });

    }

}
