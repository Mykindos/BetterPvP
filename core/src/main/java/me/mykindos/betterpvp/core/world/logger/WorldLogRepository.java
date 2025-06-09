package me.mykindos.betterpvp.core.world.logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;
import me.mykindos.betterpvp.core.database.query.values.BlobStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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

    @Inject
    public WorldLogRepository(Database database) {
        this.database = database;

        UtilServer.runTaskTimer(JavaPlugin.getPlugin(Core.class), () -> {
            purgeLogs(7);
        }, 100L, 20 * 60 * 60L);
    }

    /**
     * Saves a list of world logs to the database using a transaction.
     * Creates both main log entries and associated metadata entries.
     * All logs are saved with a unique UUID to link metadata.
     *
     * @param logs List of WorldLog objects to save
     */
    public void saveLogs(List<WorldLog> logs) {
        List<List<StatementValue<?>>> logRows = new ArrayList<>();
        List<List<StatementValue<?>>> metadataRows = new ArrayList<>();

        for (WorldLog log : logs) {
            UUID uuid = UUID.randomUUID();

            // Add main log row
            logRows.add(List.of(
                    new UuidStatementValue(uuid),
                    new StringStatementValue(Core.getCurrentServer()),
                    new StringStatementValue(log.getWorld()),
                    new IntegerStatementValue(log.getBlockX()),
                    new IntegerStatementValue(log.getBlockY()),
                    new IntegerStatementValue(log.getBlockZ()),
                    new StringStatementValue(log.getAction()),
                    new StringStatementValue(log.getMaterial()),
                    new StringStatementValue(log.getBlockData() == null ? null : log.getBlockData().getAsString()),
                    new BlobStatementValue(log.getItemStack() != null && !log.getItemStack().getType().isAir() ? log.getItemStack().serializeAsBytes() : null),
                    new TimestampStatementValue(log.getTime())
            ));

            // Add metadata rows
            if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
                log.getMetadata().forEach((key, value) -> {
                    metadataRows.add(List.of(
                            new UuidStatementValue(uuid),
                            new StringStatementValue(key),
                            new StringStatementValue(value)
                    ));
                });
            }
        }

        List<Statement> statements = new ArrayList<>();

        // Create bulk insert for logs
        if (!logRows.isEmpty()) {
            statements.add(Statement.builder()
                    .insertInto("world_logs", "id", "Server", "World", "BlockX", "BlockY", "BlockZ", "Action", "Material", "BlockData", "ItemStack", "Time")
                    .valuesBulk(logRows)
                    .build());
        }


        if (!metadataRows.isEmpty()) {
            statements.add(Statement.builder()
                    .insertInto("world_logs_metadata", "LogId", "MetaKey", "MetaValue")
                    .valuesBulk(metadataRows)
                    .build());
        }

        database.executeTransaction(statements, TargetDatabase.GLOBAL);

    }

    /**
     * Processes a world log session by executing the stored query and populating results.
     * Handles pagination and calculates total pages available.
     * Deserializes stored metadata and item stacks from the database.
     *
     * @param session The session containing the query and storage for results
     * @param page    Current page number to process (1-based)
     */
    public void processSession(WorldLogSession session, int page) {

        session.setData(new ArrayList<>());

        // Change the offset
        if (session.getStatement().isHasOffset()) {
            session.getStatement().getValues().removeLast();
            session.getStatement().getValues().add(new IntegerStatementValue((page - 1) * 10));
        }

        try (ResultSet results = database.executeQuery(session.getStatement(), TargetDatabase.GLOBAL).join()) {
            while (results.next()) {
                UUID id = UUID.fromString(results.getString(1));
                String world = results.getString(3);
                int x = results.getInt(4);
                int y = results.getInt(5);
                int z = results.getInt(6);
                String action = results.getString(7);
                String material = results.getString(8);
                String blockData = results.getString(9);
                byte[] itemStack = results.getBytes(10);
                Instant time = results.getTimestamp(11).toInstant();
                String metadataJson = results.getString(12);
                int count = results.getInt(13);

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
    public Statement getStatementForBlock(Block block) {
        return Statement.builder().queryBase(getBasicQueryBase()).where("Server", "=", StringStatementValue.of(Core.getCurrentServer()))
                .where("World", "=", StringStatementValue.of(block.getWorld().getName()))
                .where("BlockX", "=", IntegerStatementValue.of(block.getX()))
                .where("BlockY", "=", IntegerStatementValue.of(block.getY()))
                .where("BlockZ", "=", IntegerStatementValue.of(block.getZ()))
                .orderBy("Time", false)
                .limit(10)
                .offset(0)
                .build();
    }

    public void purgeLogs(int days) {
        Instant cutoff = Instant.now().minusSeconds(days * 24L * 60L * 60L);
        Statement statement = new Statement("DELETE LOW_PRIORITY FROM world_logs WHERE Server = ? AND Time <= ? LIMIT ?",
                StringStatementValue.of(Core.getCurrentServer()),
                new TimestampStatementValue(cutoff),
                new LongStatementValue(50000L));

        database.executeUpdateNoTimeout(statement, TargetDatabase.GLOBAL).thenAccept(a -> {
            log.info("Finished purging world_logs").submit();
        }).exceptionally(ex -> {
            log.error("Failed to purge world_logs", ex);
            return null;
        });
    }

    /**
     * Provides the base SQL query for retrieving world logs.
     * Includes metadata aggregation using JSON_ARRAYAGG for efficient retrieval.
     * Calculates total count for pagination purposes.
     *
     * @return Base SQL query string
     */
    public String getBasicQueryBase() {
        return """
                SELECT
                    world_logs.*,
                    (
                        SELECT JSON_ARRAYAGG(
                                       JSON_OBJECT(
                                               'Key', wlm.MetaKey,
                                               'Value', wlm.MetaValue
                                       )
                               )
                        FROM world_logs_metadata wlm
                        WHERE wlm.LogId = world_logs.id
                    ) AS metadata,
                    COUNT(*) OVER() AS total
                FROM world_logs
                """;
    }

}
