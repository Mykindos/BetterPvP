package me.mykindos.betterpvp.core.logging.appenders;

import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.LogAppender;
import me.mykindos.betterpvp.core.logging.PendingLog;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class DatabaseAppender implements LogAppender {

    private final Database database;
    private final String server;

    public DatabaseAppender(@NotNull Database database, Core core) {
        this.database = database;
        this.server = core.getConfig().getString("tab.server");
    }

    @SneakyThrows
    @Override
    public void append(PendingLog pendingLog) {

        if(pendingLog.getLevel().equalsIgnoreCase("ERROR")) {
            return;
        }

        StringBuilder message = new StringBuilder(pendingLog.getMessage());
        for(Object arg : pendingLog.getArgs()) {
            if(arg instanceof Throwable throwable) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                message.append("\n").append(sw);
            }
        }

        String finalMessage = message.toString();
        if (finalMessage.length() > 65535) {
            finalMessage = finalMessage.substring(0, 65535 - 3) + "...";
        }


        database.executeUpdate(new Statement("INSERT INTO logs (id, Server, Level, Action, Message, Time) VALUES (?, ?, ?, ?, ?, ?)",
                new UuidStatementValue(pendingLog.getId()),
                new StringStatementValue(server),
                new StringStatementValue(pendingLog.getLevel()),
                new StringStatementValue(pendingLog.getAction()),
                new StringStatementValue(finalMessage),
                new LongStatementValue(pendingLog.getTime())
        ), TargetDatabase.GLOBAL).thenRunAsync(() -> {
            if(!pendingLog.getContext().isEmpty()) {
                List<Statement> contextBatch = new ArrayList<>();
                pendingLog.getContext().forEach((key, value) -> {
                    contextBatch.add(new Statement("INSERT INTO logs_context (LogID, Context, Value) VALUES (?, ?, ?)",
                            new UuidStatementValue(pendingLog.getId()),
                            new StringStatementValue(key),
                            new StringStatementValue(value)
                    ));
                });
                database.executeBatch(contextBatch, TargetDatabase.GLOBAL);
            }
        });




    }

}
