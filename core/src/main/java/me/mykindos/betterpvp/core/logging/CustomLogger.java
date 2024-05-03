package me.mykindos.betterpvp.core.logging;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessageFactory;

import java.util.UUID;

public class CustomLogger {

    private final String name;

    public CustomLogger(String name) {
        this.name = name;
    }

    /**
     * logs to the database
     * @param level the level of the log
     * @param message the message
     * @param args args the message specifies
     * @return the UUID of the log
     */
    public PendingLog log(String level, String message, Object... args) {
        Message test = ReusableMessageFactory.INSTANCE.newMessage(message, args);
        return new PendingLog(UUID.randomUUID(), name, level, test.getFormattedMessage(), System.currentTimeMillis(), args);
    }

    /**
     * logs an INFO level message to the database
     *
     * @param message the message
     * @param args    args the message specifies
     * @return the UUID of the log
     */
    public PendingLog info(String message, Object... args) {
        return log("INFO", message, args);
    }

    /**
     * logs an ERROR level message to the database
     * @param message the message
     * @param args args the message specifies
     * @return the UUID of the log
     */
    public PendingLog error(String message, Object... args) {
        return log("ERROR", message, args);
    }

    /**
     * logs a WARN level message to the database
     * @param message the message
     * @param args args the message specifies
     * @return the UUID of the log
     */
    public PendingLog warn(String message, Object... args) {
        return log("WARN", message, args);
    }

    /**
     * logs an TRACE level message to the database
     * @param message the message
     * @param args args the message specifies
     * @return the UUID of the log
     */
    public PendingLog trace(String message, Object... args) {
        return log("TRACE", message, args);
    }
}
