package me.mykindos.betterpvp.core.listener.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

@CustomLog
public class ListenerLoader extends Loader {

    /**
     * A final instance of the Adapters class used to validate and manage the loading of adapters
     * for classes annotated with PluginAdapter. This field is used extensively in the ListenerLoader
     * class to ensure that all required dependencies for listeners are satisfied before loading them.
     */
    private final Adapters adapters;

    /**
     * Constructs a new instance of ListenerLoader.
     *
     * @param plugin the main plugin instance to associate with the loader.
     */
    public ListenerLoader(BPvPPlugin plugin) {
        super(plugin);
        this.adapters = new Adapters(plugin);
    }

    /**
     * Registers a Bukkit {@link Listener} with the plugin's internal listener management and the Bukkit event dispatcher.
     * If the listener is not already registered, it will be added to the plugin's listener list
     * and registered with the Bukkit PluginManager for event handling.
     *
     * @param listener the listener to be registered
     */
    public void register(Listener listener) {
        if (!plugin.getListeners().contains(listener)) {
            plugin.getListeners().add(listener);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            count++;
        }
    }

    /**
     * Loads and registers a listener by injecting its dependencies and
     * adding it to the plugin's listener system.
     *
     * @param listener the Listener instance to be loaded and registered
     */
    public void load(Listener listener) {
        plugin.getInjector().injectMembers(listener);
        register(listener);
    }

    /**
     * Attempts to load a listener class by checking if it can be loaded, resolving its dependencies,
     * and invoking the loading process. If the class cannot be loaded (e.g., due to missing dependencies),
     * a warning is logged. If an exception occurs during the loading process, the error is logged.
     *
     * @param clazz the class of the listener to be loaded
     */
    @Override
    public void load(Class<?> clazz) {
        if (!adapters.canLoad(clazz)) {
            log.warn("Could not load listener " + clazz.getSimpleName() + "! Dependencies not found!").submit();
            return;
        }

        try {
            Listener listener = (Listener) plugin.getInjector().getInstance(clazz);
            load(listener);
        } catch (Exception ex) {
            log.error("Failed to load listener", ex).submit();
        }
    }

}
