package me.mykindos.betterpvp.core.logging;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessageFactory;

public class CustomLogger {

    private final String name;

    public CustomLogger(String name) {
        this.name = name;
    }

    public void log(String level, String message, Object... args) {
        Message test = ReusableMessageFactory.INSTANCE.newMessage(message, args);
        LoggerFactory.getInstance().addLog(new PendingLog(name, level, test.getFormattedMessage(), System.currentTimeMillis(), args));
    }

    public void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    public void warn(String message, Object... args) {
        log("WARN", message, args);
    }

    public void trace(String message, Object... args) {
        log("TRACE", message, args);
    }
}
