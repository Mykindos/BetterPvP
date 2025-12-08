package me.mykindos.betterpvp.clans.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.listener.loader.ListenerLoader;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
public class ClansListenerLoader extends ListenerLoader {

    @Inject
    public ClansListenerLoader(Clans plugin) {
        super(plugin);
    }

    public void registerListeners(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(BPvPListener.class);
        for (var clazz : classes) {
            if (Listener.class.isAssignableFrom(clazz)) {
                if(!Modifier.isAbstract(clazz.getModifiers())) {
                    load(clazz);
                }
            }
        }

        Set<Class<? extends Reloadable>> reloadHooks = reflections.getSubTypesOf(Reloadable.class);
        final Adapters adapters = new Adapters(plugin);
        for (var hookClass : reloadHooks) {
            if (adapters.canLoad(hookClass) && !Modifier.isAbstract(hookClass.getModifiers())) {
                final Reloadable hook = plugin.getInjector().getInstance(hookClass);
                hook.reload();
                plugin.getReloadables().add(hook);
            }
        }

        plugin.getLogger().info("Loaded " + count + " listeners for " + packageName);
    }
}
