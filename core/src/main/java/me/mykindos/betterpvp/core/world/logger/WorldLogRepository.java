package me.mykindos.betterpvp.core.world.logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BlobStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class WorldLogRepository {

    private final Database database;
    private static final Gson GSON = new Gson();


    @Inject
    @Config(path = "tab.server", defaultValue = "Clans-1")
    private String server;

    @Inject
    public WorldLogRepository(Database database) {
        this.database = database;
    }

    public void saveLogs(List<WorldLog> logs) {
        List<Statement> statements = new ArrayList<>();
        for (WorldLog log : logs) {

            UUID uuid = UUID.randomUUID();
            String query = "INSERT INTO world_logs (id, Server, World, BlockX, BlockY, BlockZ, Action, Material, BlockData, ItemStack, Time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            statements.add(new Statement(query,
                    new UuidStatementValue(uuid),
                    new StringStatementValue(server),
                    new StringStatementValue(log.getWorld()),
                    new IntegerStatementValue(log.getBlockX()),
                    new IntegerStatementValue(log.getBlockY()),
                    new IntegerStatementValue(log.getBlockZ()),
                    new StringStatementValue(log.getAction()),
                    new StringStatementValue(log.getMaterial()),
                    new StringStatementValue(log.getBlockData() == null ? null : log.getBlockData().getAsString()),
                    new BlobStatementValue(log.getItemStack() != null && !log.getItemStack().getType().isAir() ? log.getItemStack().serializeAsBytes() : null),
                    new TimestampStatementValue(log.getTime())));

            if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
                continue;
            }

            log.getMetadata().forEach((key, value) -> {
                String metadataQuery = "INSERT INTO world_logs_metadata (LogId, MetaKey, MetaValue) VALUES (?, ?, ?)";
                statements.add(new Statement(metadataQuery,
                        new UuidStatementValue(uuid),
                        new StringStatementValue(key),
                        new StringStatementValue(value)));
            });
        }

        database.executeTransaction(statements, TargetDatabase.GLOBAL);
    }

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

    public Statement getStatementForBlock(Block block) {
        return Statement.builder().queryBase(getBasicQueryBase()).where("Server", "=", StringStatementValue.of(server))
                .where("World", "=", StringStatementValue.of(block.getWorld().getName()))
                .where("BlockX", "=", IntegerStatementValue.of(block.getX()))
                .where("BlockY", "=", IntegerStatementValue.of(block.getY()))
                .where("BlockZ", "=", IntegerStatementValue.of(block.getZ()))
                .orderBy("Time", false)
                .limit(10)
                .offset(0)
                .build();
    }

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

    public String getAdvancedQueryBase() {
        return """
                WITH CandidateLogs AS (
                    SELECT wl.id, Count(wl.id) OVER() AS total
                    FROM world_logs wl
                             LEFT JOIN world_logs_metadata wlm1
                                  ON wl.id = wlm1.LogId
                    WHERE ((wlm1.MetaKey = 'PlayerName'
                      AND wlm1.MetaValue = 'Player103'))
                      AND wl.Server = 'ServerClans-1'
                      AND wl.World = 'Worldworld'
                    ORDER BY wl.Time DESC
                    LIMIT 10
                )
                SELECT
                    wl.*,
                    JSON_ARRAYAGG(JSON_OBJECT('Key', wlm_all.MetaKey, 'Value', wlm_all.MetaValue)) AS metadata,
                    cl.total
                FROM CandidateLogs cl
                         JOIN world_logs wl ON wl.id = cl.id
                         JOIN world_logs_metadata wlm_all ON wl.id = wlm_all.LogId
                GROUP BY wl.id
                ORDER BY wl.Time DESC;
                """;
    }
}
