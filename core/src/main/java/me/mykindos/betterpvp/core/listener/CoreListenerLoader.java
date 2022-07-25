package me.mykindos.betterpvp.core.listener;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Due to the way spigot loads plugins, this code needs to be duplicated in order to use reflection properly
 */
public record CoreListenerLoader(BPvPPlugin plugin) {

    public void registerListeners(String packageName) {
        int count = 0;
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(BPvPListener.class);
        for (var clazz : classes) {
            if (Listener.class.isAssignableFrom(clazz)) {
                try {
                    Listener listener = (Listener) clazz.getDeclaredConstructor().newInstance();
                    plugin.getInjector().injectMembers(listener);
                    plugin.getListeners().add(listener);
                    Bukkit.getPluginManager().registerEvents(listener, plugin);

                    count++;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("Loaded " + count + " listeners for " + packageName);
    }
}
