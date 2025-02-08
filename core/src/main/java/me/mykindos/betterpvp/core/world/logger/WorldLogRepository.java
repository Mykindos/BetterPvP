package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
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

            if(log.getMetadata() == null || log.getMetadata().isEmpty()){
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
}
