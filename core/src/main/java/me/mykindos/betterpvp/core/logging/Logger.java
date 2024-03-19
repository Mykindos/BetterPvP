package me.mykindos.betterpvp.core.logging;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
@Slf4j
public class Logger {

    private static Database database;

    @Inject
    public Logger(Database database) {
        Logger.database = database;
    }

    public static UUID log(String level, String message, Object... args) {
        assert database != null;

        String logMessage = String.format(message, args).replaceAll("<.*>", "");
        log.info(logMessage);
        UUID logID = UUID.randomUUID();
        database.executeUpdate(new Statement("INSERT INTO logs (id, Level, Message, Time) VALUES (?, ?, ?, ?)",
                new UuidStatementValue(logID),
                new StringStatementValue(level),
                new StringStatementValue(logMessage),
                new LongStatementValue(System.currentTimeMillis())
        ));
        return logID;
    }

    public static UUID info(String message, Object... args) {
        return log("INFO", message, args);
    }

    public static UUID error(String message, Object... args) {
        return log("ERROR", message, args);
    }

    public static UUID warn(String message, Object... args) {
       return log("WARN", message, args);
    }

    public static UUID trace(String message, Object... args) {
        return log("TRACE", message, args);
    }

}
