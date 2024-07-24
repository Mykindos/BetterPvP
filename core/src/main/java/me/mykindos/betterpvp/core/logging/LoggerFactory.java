package me.mykindos.betterpvp.core.logging;

import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import net.kyori.adventure.text.Component;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@CustomLog
public class LoggerFactory {

    private static final LoggerFactory INSTANCE = new LoggerFactory();

    private final Set<LogAppender> appenders = new HashSet<>();
    private final Set<ILogFormatter> formatters = new HashSet<>();
    private final Queue<PendingLog> logs = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private LoggerFactory() {
        executor.scheduleAtFixedRate(() -> {
            if (appenders.isEmpty()) return;
            if (logs.isEmpty()) return;

            PendingLog log = logs.poll();
            appenders.forEach(appender -> appender.append(log));

        }, 0, 25, TimeUnit.MILLISECONDS);

        loadFormatters();
    }

    @SneakyThrows
    private void loadFormatters() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends ILogFormatter>> classes = reflections.getSubTypesOf(ILogFormatter.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum() || clazz.isAnnotationPresent(Deprecated.class))
                continue;
            formatters.add(clazz.getConstructor().newInstance());
        }

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

    public Component formatLog(CachedLog cachedLog) {
        if (cachedLog.getAction() != null && cachedLog.getContext() != null) {
            for (ILogFormatter formatter : formatters) {
                if (formatter.getAction().equals(cachedLog.getAction())) {
                    return formatter.formatLog(cachedLog.getContext());
                }
            }

            log.warn("No formatter found for action: " + cachedLog.getAction()).submit();
        }

        return null;
    }

}
