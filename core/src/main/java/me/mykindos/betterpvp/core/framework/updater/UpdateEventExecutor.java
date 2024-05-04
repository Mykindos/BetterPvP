package me.mykindos.betterpvp.core.framework.updater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
@Singleton
public class UpdateEventExecutor {

    private final Core core;

    public Map<Long, Integer> lastRunTimers = new HashMap<>();
    public List<UpdateEventContainer> updateEvents = new ArrayList<>();

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
            for (var method : methods) {

                var updateEvent = method.getAnnotation(UpdateEvent.class);
                if (updateEvent == null) continue;

                updateEvents.add(new UpdateEventContainer(listener, method, updateEvent));

            }

        }

        Collections.sort(updateEvents);

    }

    private void executeUpdateEvents() {

        var updateTimers = new HashMap<Long, Integer>();
        int currentTick = Bukkit.getCurrentTick();

        for(var entry : updateEvents) {
            var event = entry.getUpdateEvent();
            int tickDelay = (int) event.delay() / 50;
            Integer lastRunTick = lastRunTimers.get(event.delay());
            if (lastRunTick != null) {
                if (lastRunTick <= currentTick) {
                    callUpdater(event, entry.getMethod(), entry.getInstance());

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

        lastRunTimers.putAll(updateTimers);
    }


    private void callUpdater(UpdateEvent updateEvent, Method method, Object obj) {
        UtilServer.runTask(core, updateEvent.isAsync(), () -> executeMethod(method, obj));

    }

    private void executeMethod(Method method, Object obj) {
        try {
            method.invoke(obj);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            log.error("Could not execute updater {} in {}", method.getName(), method.getDeclaringClass().getName(), e).submit();
        }
    }
}
