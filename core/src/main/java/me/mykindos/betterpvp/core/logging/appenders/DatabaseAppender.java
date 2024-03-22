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
import java.util.UUID;

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

        database.executeUpdate(new Statement("INSERT INTO logs (id, Level, Message, Time) VALUES (?, ?, ?, ?)",
                new UuidStatementValue(UUID.randomUUID()),
                new StringStatementValue(pendingLog.getLevel()),
                new StringStatementValue(message.toString()),
                new LongStatementValue(pendingLog.getTime())
        ));
    }

}
