package me.mykindos.betterpvp.core.logging;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class Logger {

    private static Database database;

    @Inject
    public Logger(Database database) {
        Logger.database = database;
    }

    public static void log(String level, String message, Object... args) {
        assert database != null;

        String logMessage = String.format(message, args);

        database.executeUpdate(new Statement("INSERT INTO logs (Level, Message) VALUES (?, ?)",
                new StringStatementValue(level),
                new StringStatementValue(logMessage)
        ));
    }

    public static void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public static void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    public static void warn(String message, Object... args) {
        log("WARN", message, args);
    }

    public static void trace(String message, Object... args) {
        log("TRACE", message, args);
    }

}
