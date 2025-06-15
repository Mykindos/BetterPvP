package me.mykindos.betterpvp.core.listener.loader;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
@CustomLog
public class CoreListenerLoader extends ListenerLoader{

    @Inject
    public CoreListenerLoader(Core plugin) {
        super(plugin);
    }

    public void registerListeners(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(BPvPListener.class);
        for (var clazz : classes) {
            if (Listener.class.isAssignableFrom(clazz)) {
                load(clazz);
            }
        }

        Set<Class<? extends ReloadHook>> reloadHooks = reflections.getSubTypesOf(ReloadHook.class);
        for (var hookClass : reloadHooks) {
            if (!Modifier.isAbstract(hookClass.getModifiers())) {
                final ReloadHook hook = plugin.getInjector().getInstance(hookClass);
                hook.reload();
                plugin.getReloadHooks().add(hook);
            }
        }

        plugin.getLogger().info("Loaded " + count + " listeners for " + packageName);
    }
}
