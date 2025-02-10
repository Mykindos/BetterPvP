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
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
            String query = "INSERT INTO world_logs (id, Server, World, BlockX, BlockY, BlockZ, Action, Material) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            statements.add(new Statement(query,
                    new UuidStatementValue(uuid),
                    new StringStatementValue(server),
                    new StringStatementValue(log.getWorld()),
                    new IntegerStatementValue(log.getBlockX()),
                    new IntegerStatementValue(log.getBlockY()),
                    new IntegerStatementValue(log.getBlockZ()),
                    new StringStatementValue(log.getAction()),
                    new StringStatementValue(log.getMaterial())));

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

        database.executeTransaction(statements, true, TargetDatabase.GLOBAL);
    }

    public void processSession(Player player, WorldLogSession session, int page) {
        CompletableFuture.runAsync(() -> {
            session.setData(new ArrayList<>());

            // Change the offset
            session.getStatement().getValues().removeLast();
            session.getStatement().getValues().add(new IntegerStatementValue(page * 10));

            try (ResultSet results = database.executeQuery(session.getStatement(), TargetDatabase.GLOBAL)) {
                while (results.next()) {
                    UUID id = UUID.fromString(results.getString(1));
                    String world = results.getString(3);
                    int x = results.getInt(4);
                    int y = results.getInt(5);
                    int z = results.getInt(6);
                    String action = results.getString(7);
                    String material = results.getString(8);
                    Instant time = results.getTimestamp(9).toInstant();
                    String metadataJson = results.getString(10);
                    int count = results.getInt(11);

                    session.setPages((int) Math.ceil(count / 10.0));

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


                    session.getData().add(log);
                }
            } catch (Exception e) {
                log.error("Error processing session", e).submit();
            }

        }).thenAcceptAsync((v) -> {
            for (WorldLog log : session.getData()) {
                UtilMessage.message(player, Component.text(log.getTime().toString() + " - " + log.getAction() + " " + log.getMaterial() + " at " + log.getBlockX() + " " + log.getBlockY() + " " + log.getBlockZ()));
            }
            UtilMessage.simpleMessage(player, "Page " + session.getCurrentPage() + " of " + session.getPages());
        });


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
                                               'MetaKey', wlm.MetaKey,
                                               'MetaValue', wlm.MetaValue
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
                SELECT
                    wl.*,
                    (
                        SELECT JSON_ARRAYAGG(
                                       JSON_OBJECT(
                                               'MetaKey', wlm2.MetaKey,
                                               'MetaValue', wlm2.MetaValue
                                       )
                               )
                        FROM world_logs_metadata wlm2
                        WHERE wlm2.LogId = wlm.LogId
                    ) AS metadata,
                    COUNT(*) OVER() AS total
                FROM world_logs_metadata wlm
                INNER JOIN world_logs wl ON wl.id = wlm.LogId
                """;
    }
}
