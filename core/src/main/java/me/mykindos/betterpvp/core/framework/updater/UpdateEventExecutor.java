package me.mykindos.betterpvp.core.framework.updater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

@Slf4j
@Singleton
public class UpdateEventExecutor {

    private final Core core;

    public HashMap<Long, Integer> lastRunTimers = new HashMap<>();
    public HashMap<Object, HashMap<Method, UpdateEvent>> updateMethods = new HashMap<>();

    // Depending on server performance, an update event running every tick may skip an interval. This allows the event to rollover to the next interval.
    @Config(path = "update-event.allow-rollover", defaultValue = "false")
    @Inject
    private boolean allowRollover;

    @Inject
    public UpdateEventExecutor(Core core) {
        this.core = core;
    }

    public void initialize() {
        UtilServer.runTaskTimer(core, this::executeUpdateEvents, 0, 1);
    }

    public void loadPlugin(BPvPPlugin plugin) {
        var listeners = plugin.getListeners();

        for (var listener : listeners) {
            var methods = listener.getClass().getMethods();
            HashMap<Method, UpdateEvent> methodMap = new HashMap<>();
            for (var method : methods) {

                var updateEvent = method.getAnnotation(UpdateEvent.class);
                if (updateEvent == null) continue;

                methodMap.put(method, updateEvent);

            }

            updateMethods.put(listener, methodMap);

        }

    }

    private void executeUpdateEvents() {

        var updateTimers = new HashMap<Long, Integer>();
        int currentTick = Bukkit.getCurrentTick();

        for (var entry : updateMethods.entrySet()) {

            for (var method : entry.getValue().entrySet()) {
                var event = method.getValue();
                int tickDelay = (int) event.delay() / 50;
                Integer lastRunTick = lastRunTimers.get(event.delay());
                if (lastRunTick != null) {
                    if (lastRunTick <= currentTick) {
                        callUpdater(event, method.getKey(), entry.getKey());

                        if (allowRollover) {
                            updateTimers.put(event.delay(), currentTick + tickDelay);
                        } else {
                            if (!updateTimers.containsKey(event.delay())) {
                                updateTimers.put(event.delay(), currentTick + tickDelay);
                            }
                        }
                    }
                } else {
                    lastRunTimers.put(event.delay(), currentTick + tickDelay);
                }

            }
        }

        lastRunTimers.putAll(updateTimers);
    }


    private void callUpdater(UpdateEvent updateEvent, Method method, Object obj) {
        UtilServer.runTask(core, updateEvent.isAsync(), () -> executeMethod(method, obj));
    }

    private void executeMethod(Method method, Object obj) {
        try {
            method.invoke(obj);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            log.error("Could not execute updater {} in {}", method.getName(), method.getDeclaringClass().getName(), e);
        }
    }
}
