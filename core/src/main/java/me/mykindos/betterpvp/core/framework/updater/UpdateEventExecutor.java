package me.mykindos.betterpvp.core.framework.updater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
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

    public HashMap<Long, Long> lastRunTimers = new HashMap<>();

    @Inject
    public UpdateEventExecutor(Core core) {
        this.core = core;
    }

    public void initialize() {
        UtilServer.runTaskTimer(core, this::executeUpdateEvents, 0, 1);
    }

    private void executeUpdateEvents() {

        var updateTimers = new HashMap<Long, Long>();

        for (var plugin : Bukkit.getPluginManager().getPlugins()) {

            if (!(plugin instanceof BPvPPlugin bPvPPlugin)) continue;
            var listeners = bPvPPlugin.getListeners();

            for (var listener : listeners) {

                var methods = listener.getClass().getDeclaredMethods();

                for (var method : methods) {

                    var updateEvent = method.getAnnotation(UpdateEvent.class);
                    if (updateEvent == null) continue;

                    if (lastRunTimers.containsKey(updateEvent.delay())) {
                        if (lastRunTimers.get(updateEvent.delay()) < System.currentTimeMillis()) {

                            callUpdater(updateEvent, method, listener);
                            updateTimers.put(updateEvent.delay(), System.currentTimeMillis() + updateEvent.delay());
                        }
                    } else {
                        lastRunTimers.put(updateEvent.delay(), System.currentTimeMillis() + updateEvent.delay());
                    }
                }
            }
        }

        for (var update : updateTimers.entrySet()) {
            lastRunTimers.put(update.getKey(), update.getValue());
        }
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
