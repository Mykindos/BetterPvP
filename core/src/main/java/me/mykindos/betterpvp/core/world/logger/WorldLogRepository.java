package me.mykindos.betterpvp.core.world.logger;

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

import java.sql.PreparedStatement;
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

    public void processSession(Player player, WorldLogSession session) {
        CompletableFuture.runAsync(() -> {
            session.setData(new ArrayList<>());
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

                    HashMap<String, String> metadata = new HashMap<>();
                    try (ResultSet metadataResults = database.executeQuery(new Statement("SELECT * FROM world_logs_metadata WHERE LogId = ?", new UuidStatementValue(id)), TargetDatabase.GLOBAL)) {
                        while (metadataResults.next()) {
                            String key = metadataResults.getString(2);
                            String value = metadataResults.getString(3);
                            metadata.put(key, value);
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
                UtilMessage.message(player, Component.text(log.getAction() + " " + log.getMaterial() + " at " + log.getBlockX() + " " + log.getBlockY() + " " + log.getBlockZ()));
            }
        });
        ;
    }

    public Statement getStatementForBlock(Block block) {
        return new Statement("SELECT *, Count(*) OVER() FROM world_logs WHERE Server = ? AND World = ? AND BlockX = ? AND BlockY = ? AND BlockZ = ? ORDER BY Time DESC LIMIT 10 OFFSET ?",
                new StringStatementValue(server),
                new StringStatementValue(block.getWorld().getName()),
                new IntegerStatementValue(block.getX()),
                new IntegerStatementValue(block.getY()),
                new IntegerStatementValue(block.getZ()),
                new IntegerStatementValue(0));
    }
}
