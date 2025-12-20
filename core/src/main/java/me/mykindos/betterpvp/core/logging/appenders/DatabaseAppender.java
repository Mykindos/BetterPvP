package me.mykindos.betterpvp.core.logging.appenders;

import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.records.LogsContextRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.LogsRecord;
import me.mykindos.betterpvp.core.logging.LogAppender;
import me.mykindos.betterpvp.core.logging.PendingLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static me.mykindos.betterpvp.core.database.jooq.Tables.LOGS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.LOGS_CONTEXT;

@CustomLog
public class DatabaseAppender implements LogAppender {

    private final Database database;
    private static final Executor LOG_EXECUTOR = Executors.newSingleThreadExecutor();
    private final List<PendingLogEntry> pendingLogs = Collections.synchronizedList(new ArrayList<>());

    public DatabaseAppender(@NotNull Database database, Core core) {
        this.database = database;
        // Start the batch processing timer - runs every 30 seconds (600 ticks)
        UtilServer.runTaskTimer(core, this::processBatchedLogs, 600L, 600L);

    }

    @SneakyThrows
    @Override
    public void append(PendingLog pendingLog) {

        if (pendingLog.getLevel().equalsIgnoreCase("ERROR")) {
            return;
        }

        StringBuilder message = new StringBuilder(pendingLog.getMessage() != null ? pendingLog.getMessage() : "");
        final Object[] args = pendingLog.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Throwable throwable) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    message.append("\n").append(sw);
                }
            }
        }

        String finalMessage = message.toString();
        if (finalMessage.length() > 65535) {
            finalMessage = finalMessage.substring(0, 65535 - 3) + "...";
        }

        // Add to batch queue instead of immediate processing
        pendingLogs.add(new PendingLogEntry(pendingLog, finalMessage));

    }

    private void processBatchedLogs() {
        if (pendingLogs.isEmpty()) {
            return;
        }

        // Copy and clear the pending logs atomically
        List<PendingLogEntry> logsToProcess;
        synchronized (pendingLogs) {
            logsToProcess = new ArrayList<>(pendingLogs);
            pendingLogs.clear();
        }

        log.info("Processing {} batched log entries", logsToProcess.size()).submit();

        // Process asynchronously to avoid blocking the timer thread
        CompletableFuture.runAsync(() -> {
            try {
                insertBatchedLogs(logsToProcess);
            } catch (Exception e) {
                log.error("Failed to process batched logs", e).submit();

            }
        });
    }

    private void insertBatchedLogs(List<PendingLogEntry> logEntries) {
        CompletableFuture.runAsync(() -> {
            try {
                database.getDslContext().transaction(config -> {
                    DSLContext ctx = DSL.using(config);

                    // Prepare log records
                    List<LogsRecord> logRecords = new ArrayList<>();
                    List<LogsContextRecord> contextRecords = new ArrayList<>();

                    for (PendingLogEntry entry : logEntries) {
                        final PendingLog pl = entry.pendingLog;
                        final String level = pl.getLevel() != null ? pl.getLevel() : "INFO";
                        final String action = pl.getAction() != null ? pl.getAction() : "";
                        final String message = entry.finalMessage != null ? entry.finalMessage : "";

                        // Create log record
                        LogsRecord logRecord = ctx.newRecord(LOGS);
                        logRecord.setId(pl.getId());
                        logRecord.setRealm(Core.getCurrentRealm().getRealm());
                        logRecord.setLevel(level);
                        logRecord.setAction(action);
                        logRecord.setMessage(message);
                        logRecord.setLogTime(pl.getTime());
                        logRecords.add(logRecord);

                        // Create context records
                        final java.util.Map<String, String> ctx_map = pl.getContext();
                        if (ctx_map != null && !ctx_map.isEmpty()) {
                            ctx_map.forEach((key, value) -> {
                                LogsContextRecord contextRecord = ctx.newRecord(LOGS_CONTEXT);
                                contextRecord.setLogId(pl.getId());
                                contextRecord.setRealm(Core.getCurrentRealm().getRealm());
                                contextRecord.setContext(key != null ? key : "");
                                contextRecord.setValue(value != null ? value : "");
                                contextRecords.add(contextRecord);
                            });
                        }
                    }

                    // Batch insert logs
                    if (!logRecords.isEmpty()) {

                        ctx.batchInsert(logRecords).execute();
                    }

                    // Batch insert contexts
                    if (!contextRecords.isEmpty()) {
                        ctx.batchInsert(contextRecords).execute();
                    }
                });
            } catch (Exception ex) {
                log.error("Failed to insert batched logs", ex).submit();
            }
        }, LOG_EXECUTOR);
    }

    /**
     * Helper class to hold a pending log and its processed message
     */
    private static class PendingLogEntry {
        final PendingLog pendingLog;
        final String finalMessage;

        PendingLogEntry(PendingLog pendingLog, String finalMessage) {
            this.pendingLog = pendingLog;
            this.finalMessage = finalMessage;
        }
    }


}
