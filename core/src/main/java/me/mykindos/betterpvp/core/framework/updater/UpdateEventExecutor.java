package me.mykindos.betterpvp.core.framework.updater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

@Slf4j
@Singleton
public class UpdateEventExecutor {

    private final Core core;

    public HashMap<Long, Long> lastRunTimers = new HashMap<>();
    public HashMap<Object, HashMap<Method, UpdateEvent>> updateMethods = new HashMap<>();

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

        var updateTimers = new HashMap<Long, Long>();

        updateMethods.forEach((key, value) -> value.forEach((method, event) -> {
            if (lastRunTimers.containsKey(event.delay())) {
                if (lastRunTimers.get(event.delay()) < System.currentTimeMillis()) {

                    callUpdater(event, method, key);
                    updateTimers.put(event.delay(), System.currentTimeMillis() + event.delay());
                }
            } else {
                lastRunTimers.put(event.delay(), System.currentTimeMillis() + event.delay());
            }
        }));

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
