package me.mykindos.betterpvp.shops.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.listener.loader.ListenerLoader;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.shops.Shops;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
public class ShopsListenerLoader extends ListenerLoader {

    @Inject
    public ShopsListenerLoader(Shops plugin) {
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
