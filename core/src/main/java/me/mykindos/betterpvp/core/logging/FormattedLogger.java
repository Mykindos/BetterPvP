package me.mykindos.betterpvp.core.logging;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
@CustomLog
public class FormattedLogger {

    private static Database database;

    @Inject
    public FormattedLogger(Database database) {
        FormattedLogger.database = database;
    }

    /**
     * @param formattedMessage the formatted message. Will log a message without
     * @param args the args for the message
     */
    public static UUID log(String level, String formattedMessage, Object... args) {
        String message = formattedMessage.replaceAll("<[a-zA-Z/]*?>", "");
        return log(level, message, formattedMessage, args);
    }

    public static UUID log(String level, String message, String formattedMessage, Object... args) {
        UUID id = log.log(level, message, args);
        assert database != null;

        String logMessage = String.format(formattedMessage, args);
        database.executeUpdate(new Statement("INSERT INTO formattedlogs (id, Message) VALUES (?, ?)",
                new UuidStatementValue(id),
                new StringStatementValue(logMessage)
        ));
        return id;
    }

    public static UUID info(String formattedMessage, Object... args) {
        return log("INFO", formattedMessage, args);
    }

    public static UUID error(String formattedMessage, Object... args) {
        return log("ERROR", formattedMessage, args);
    }

    public static UUID warn(String formattedMessage, Object... args) {
       return log("WARN", formattedMessage, args);
    }

    public static UUID trace(String formattedMessage, Object... args) {
        return log("TRACE", formattedMessage, args);
    }

}
