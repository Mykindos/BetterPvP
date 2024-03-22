package me.mykindos.betterpvp.core.logging.appenders;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.logging.LogAppender;
import me.mykindos.betterpvp.core.logging.PendingLog;

@Slf4j
public class LegacyAppender implements LogAppender {
    @Override
    public void append(PendingLog pendingLog) {
        switch (pendingLog.getLevel()) {
            case "ERROR":
                log.error(pendingLog.getMessage(), pendingLog.getArgs());
                break;
            case "WARN":
                log.warn(pendingLog.getMessage(), pendingLog.getArgs());
                break;
            case "TRACE":
                log.trace(pendingLog.getMessage(), pendingLog.getArgs());
                break;
            default:
                log.info(pendingLog.getMessage(), pendingLog.getArgs());
        }
    }
}
