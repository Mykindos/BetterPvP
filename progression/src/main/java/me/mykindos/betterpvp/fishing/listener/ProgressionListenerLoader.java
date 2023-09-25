package me.mykindos.betterpvp.fishing.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.fishing.Progression;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.listener.loader.ListenerLoader;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
public class ProgressionListenerLoader extends ListenerLoader {

    @Inject
    public ProgressionListenerLoader(Progression plugin) {
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

        plugin.getLogger().info("Loaded " + count + " listeners for " + packageName);
    }
}
