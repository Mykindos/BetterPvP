package me.mykindos.betterpvp.core.logging.appenders;

import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
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

    public DatabaseAppender(@NotNull Database database) {
        this.database = database;
    }

    @SneakyThrows
    @Override
    public void append(PendingLog pendingLog) {

        StringBuilder message = new StringBuilder(pendingLog.getMessage());
        for(Object arg : pendingLog.getArgs()) {
            if(arg instanceof Throwable throwable) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                message.append("\n").append(sw);
            }
        }

        database.executeUpdate(new Statement("INSERT INTO logs (id, Level, Action, Message, Time) VALUES (?, ?, ?, ?, ?)",
                new UuidStatementValue(pendingLog.getId()),
                new StringStatementValue(pendingLog.getLevel()),
                new StringStatementValue(pendingLog.getAction()),
                new StringStatementValue(message.toString()),
                new LongStatementValue(pendingLog.getTime())
        ));

        if(!pendingLog.getContext().isEmpty()) {
            List<Statement> contextBatch = new ArrayList<>();
            pendingLog.getContext().forEach((key, value) -> {
                contextBatch.add(new Statement("INSERT INTO logs_context (LogID, Context, Value) VALUES (?, ?, ?)",
                        new UuidStatementValue(pendingLog.getId()),
                        new StringStatementValue(key),
                        new StringStatementValue(value)
                ));
            });
            database.executeBatch(contextBatch, false);
        }


    }

}
