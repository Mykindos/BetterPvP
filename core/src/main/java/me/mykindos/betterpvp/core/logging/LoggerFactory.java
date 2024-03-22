package me.mykindos.betterpvp.core.logging;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoggerFactory {

    private static final LoggerFactory INSTANCE = new LoggerFactory();

    private final Set<LogAppender> appenders = new HashSet<>();
    private final Queue<PendingLog> logs = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private LoggerFactory() {
        executor.scheduleAtFixedRate(() -> {
            if (appenders.isEmpty()) return;
            if (logs.isEmpty()) return;

            PendingLog log = logs.poll();
            appenders.forEach(appender -> appender.append(log));

        }, 0, 25, TimeUnit.MILLISECONDS);
    }

    public static LoggerFactory getInstance() {
        return INSTANCE;
    }

    public void close() {
        executor.shutdown();
    }

    public void addAppender(LogAppender appender) {
        appenders.add(appender);
    }

    public void removeAppender(LogAppender appender) {
        appenders.remove(appender);
    }

    public static CustomLogger createLogger(Class<?> type) {
        return new CustomLogger(type.getSimpleName());
    }

    public void addLog(PendingLog log) {
        logs.add(log);
    }

}
