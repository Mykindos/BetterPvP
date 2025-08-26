package me.mykindos.betterpvp.core.logging.appenders;

import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.LogAppender;
import me.mykindos.betterpvp.core.logging.PendingLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
        List<List<StatementValue<?>>> logRows = new ArrayList<>();
        List<List<StatementValue<?>>> contextRows = new ArrayList<>();

        // Prepare all rows
        for (PendingLogEntry entry : logEntries) {
            final PendingLog pl = entry.pendingLog;
            final String server = Core.getCurrentServer() != null ? Core.getCurrentServer() : "unknown";
            final String level = pl.getLevel() != null ? pl.getLevel() : "INFO";
            final String action = pl.getAction() != null ? pl.getAction() : "";
            final String message = entry.finalMessage != null ? entry.finalMessage : "";

            // Main log row
            logRows.add(List.of(
                    new UuidStatementValue(pl.getId()),
                    new StringStatementValue(server),
                    new StringStatementValue(level),
                    new StringStatementValue(action),
                    new StringStatementValue(message),
                    new LongStatementValue(pl.getTime())
            ));

            // Context rows
            final java.util.Map<String, String> ctx = pl.getContext();
            if (ctx != null && !ctx.isEmpty()) {
                ctx.forEach((key, value) -> {
                    contextRows.add(List.of(
                            new UuidStatementValue(pl.getId()),
                            new StringStatementValue(key != null ? key : ""),
                            new StringStatementValue(value != null ? value : "")
                    ));
                });
            }
        }

        List<Statement> statements = new ArrayList<>();

        // Create bulk insert for logs
        if (!logRows.isEmpty()) {
            statements.add(Statement.builder()
                    .lowPriority()
                    .insertInto("logs", "id", "Server", "Level", "Action", "Message", "Time")
                    .valuesBulk(logRows)
                    .build());
        }

        // Create bulk insert for context
        if (!contextRows.isEmpty()) {
            statements.add(Statement.builder()
                    .lowPriority()
                    .insertInto("logs_context", "LogID", "Context", "Value")
                    .valuesBulk(contextRows)
                    .build());
        }

        // Execute as a transaction
        database.executeTransaction(statements, TargetDatabase.GLOBAL, LOG_EXECUTOR)
                .exceptionally(throwable -> {
                    log.error("Failed to insert batched logs", throwable).submit();
                    return null;
                });

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
