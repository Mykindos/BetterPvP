package me.mykindos.betterpvp.core.logging.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.DSLContext;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.LOGS;

@Singleton
@CustomLog
public class LogRepository {

    private final Database database;

    @Inject
    public LogRepository(Database database) {
        this.database = database;

        UtilServer.runTaskTimer(JavaPlugin.getPlugin(Core.class), () -> {
            purgeLogs(7, 100000);
        }, 1000, 20 * 60 * 30);
    }

    public List<CachedLog> getLogsWithContext(String key, String value) {
        return getLogsWithContextAndAction(key, value, null);
    }

    public List<CachedLog> getLogsWithContextAndAction(String key, String value, @Nullable String actionFilter) {
        List<CachedLog> logs = new ArrayList<>();
        Statement statement1;

        if (actionFilter != null) {
            statement1 = new Statement("CALL GetLogMessagesByContextAndAction(?, ?, ?, ?, ?)",
                    new StringStatementValue(key),
                    new StringStatementValue(value),
                    new StringStatementValue(actionFilter),
                    new IntegerStatementValue(Core.getCurrentServer()),
                    new IntegerStatementValue(Core.getCurrentSeason())
            );
        } else {
            statement1 = new Statement("CALL GetLogMessagesByContextAndValue(?, ?, ?, ?)",
                    new StringStatementValue(key),
                    new StringStatementValue(value),
                    new IntegerStatementValue(Core.getCurrentServer()),
                    new IntegerStatementValue(Core.getCurrentSeason())
            );
        }

        database.executeProcedure(statement1, -1, result -> {
            try {
                while (result.next()) {
                    String message = result.getString(1);
                    String action = result.getString(2);
                    long timestamp = result.getLong(3);
                    String contextRaw = result.getString(4);

                    HashMap<String, String> contextMap = new HashMap<>();
                    String[] contexts = contextRaw.split("\\|");
                    for (String context : contexts) {
                        String[] split = context.split("::");
                        contextMap.put(split[0], split[1]);
                    }

                    if (action == null) {
                        log.warn("Fetching a log with no action, excluding. {}", contextRaw).submit();
                        continue;
                    }
                    CachedLog log = new CachedLog(message, action, timestamp, contextMap);
                    logs.add(log);
                }
            } catch (SQLException e) {
                log.error("Error fetching log data", e).submit();
            }
        }, TargetDatabase.GLOBAL).join();

        return logs;
    }

    public void purgeLogs(int days, int limit) {

        long daysToMillis = days * (24L * 60L * 60L * 1000L);

        CompletableFuture.runAsync(() -> {
            int maxRetries = 3;
            int batchSize = limit;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    DSLContext ctx = database.getDslContext();

                    int deletedRows = ctx.deleteFrom(LOGS)
                            .where(LOGS.REALM.eq(Core.getCurrentRealm()))
                            .and(LOGS.ACTION.eq(""))
                            .and(LOGS.LOG_TIME.le(System.currentTimeMillis() - daysToMillis))
                            .limit(batchSize)
                            .execute();

                    log.info("Successfully purged batch of {} logs", deletedRows).submit();

                    // Add delay between batches to reduce lock contention
                    Thread.sleep(5000); // 5 second delay
                    break; // Success, exit retry loop

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Lock wait timeout")) {
                        log.warn("Lock timeout on attempt {}, retrying in {} seconds",
                                attempt + 1, (attempt + 1) * 10).submit();

                        try {
                            Thread.sleep((attempt + 1) * 10000); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else {
                        log.error("Non-timeout error during log purge", e).submit();
                        break;
                    }
                }
            }
        });

    }
}
