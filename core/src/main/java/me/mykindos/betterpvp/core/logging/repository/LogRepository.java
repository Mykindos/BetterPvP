package me.mykindos.betterpvp.core.logging.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_LOG_MESSAGES_BY_CONTEXT_AND_ACTION;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_LOG_MESSAGES_BY_CONTEXT_AND_VALUE;
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

        try {
            DSLContext ctx = database.getDslContext();
            Result<? extends Record> result;
            if (actionFilter != null) {
                result = GET_LOG_MESSAGES_BY_CONTEXT_AND_ACTION(ctx.configuration(), key, value, actionFilter, Core.getCurrentRealm().getId());
            } else {
                result = GET_LOG_MESSAGES_BY_CONTEXT_AND_VALUE(ctx.configuration(), key, value, Core.getCurrentRealm().getId());
            }

            result.forEach(logRecord -> {
                String message = logRecord.get(0, String.class);
                String action = logRecord.get(1, String.class);
                long timestamp = logRecord.get(2, Long.class);
                String contextRaw = logRecord.get(3, String.class);

                HashMap<String, String> contextMap = new HashMap<>();
                String[] contexts = contextRaw.split("\\|");
                for (String context : contexts) {
                    String[] split = context.split("::");
                    contextMap.put(split[0], split[1]);
                }

                if (action == null) {
                    log.warn("Fetching a log with no action, excluding. {}", contextRaw).submit();
                    return;
                }
                CachedLog log = new CachedLog(message, action, timestamp, contextMap);
                logs.add(log);
            });
        } catch (Exception e) {
            log.error("Error fetching log data", e).submit();
        }

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
                            .where(LOGS.REALM.eq(Core.getCurrentRealm().getId()))
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
