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

    /**
     * Queries for all UUID items whose most recent logged location falls within the given
     * X/Z bounding box on the current realm.
     *
     * @param minX minimum X coordinate (inclusive)
     * @param minZ minimum Z coordinate (inclusive)
     * @param maxX maximum X coordinate (inclusive)
     * @param maxZ maximum Z coordinate (inclusive)
     * @return list of String arrays: [item_uuid, item_name, location_string]
     */
    public List<String[]> getUUIDItemsLastSeenInBounds(int minX, int minZ, int maxX, int maxZ) {
        List<String[]> results = new ArrayList<>();

        try {
            DSLContext ctx = database.getDslContext();

            // Use PostgreSQL DISTINCT ON to get the most recent location-bearing log per item UUID.
            // Joins logs_context three times: once for the Item UUID, once for the Location,
            // and once (left) for the ItemName.
            String sql =
                    "SELECT DISTINCT ON (lc_item.value) " +
                    "    lc_item.value  AS item_uuid, " +
                    "    lc_name.value  AS item_name, " +
                    "    lc_loc.value   AS location " +
                    "FROM logs l " +
                    "JOIN logs_context lc_item ON lc_item.log_id = l.id AND lc_item.context = 'Item' " +
                    "JOIN logs_context lc_loc  ON lc_loc.log_id  = l.id AND lc_loc.context  = 'Location' " +
                    "LEFT JOIN logs_context lc_name ON lc_name.log_id = l.id AND lc_name.context = 'ItemName' " +
                    "WHERE l.realm = ? " +
                    "  AND l.action LIKE 'ITEM_%' " +
                    "ORDER BY lc_item.value, l.log_time DESC";

            ctx.resultQuery(sql, Core.getCurrentRealm().getId()).fetch().forEach(record -> {
                String itemUuid = record.get("item_uuid", String.class);
                String itemName = record.get("item_name", String.class);
                String locationStr = record.get("location", String.class);

                if (locationStr == null) return;

                // Location format: "(WorldName, X, Y, Z)" from locationToString(loc, true, true)
                try {
                    String cleaned = locationStr.replaceAll("[)(]", "").trim();
                    String[] parts = cleaned.split(",\\s*");
                    if (parts.length < 4) return;

                    int x = (int) Math.round(Double.parseDouble(parts[1].trim()));
                    int z = (int) Math.round(Double.parseDouble(parts[3].trim()));

                    int normalMinX = Math.min(minX, maxX);
                    int normalMaxX = Math.max(minX, maxX);
                    int normalMinZ = Math.min(minZ, maxZ);
                    int normalMaxZ = Math.max(minZ, maxZ);

                    if (x >= normalMinX && x <= normalMaxX && z >= normalMinZ && z <= normalMaxZ) {
                        results.add(new String[]{itemUuid, itemName != null ? itemName : "unknown", locationStr});
                    }
                } catch (NumberFormatException ignored) {
                    // skip malformed location entries
                }
            });
        } catch (Exception e) {
            log.error("Error fetching UUID items last seen in bounds", e).submit();
        }

        return results;
    }

    public void purgeLogs(int days, int limit) {

        long daysToMillis = days * (24L * 60L * 60L * 1000L);

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            int maxRetries = 3;
            int batchSize = limit;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
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
        }).exceptionally(ex -> {
            log.error("Failed to purge logs", ex).submit();
            return null;
        });

    }
}
