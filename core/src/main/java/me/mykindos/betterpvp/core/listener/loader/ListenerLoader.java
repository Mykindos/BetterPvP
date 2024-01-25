package me.mykindos.betterpvp.core.listener.loader;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

@Slf4j
public class ListenerLoader extends Loader {

    private final Adapters adapters;

    public ListenerLoader(BPvPPlugin plugin) {
        super(plugin);
        this.adapters = new Adapters(plugin);
    }

    public void register(Listener listener) {
        plugin.getListeners().add(listener);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        count++;
    }

    public void load(Listener listener) {
        plugin.getInjector().injectMembers(listener);
        register(listener);
    }

    @Override
    public void load(Class<?> clazz) {
        if (!adapters.canLoad(clazz)) {
            log.warn("Could not load listener " + clazz.getSimpleName() + "! Dependencies not found!");
            return;
        }

        try {
            Listener listener = (Listener) plugin.getInjector().getInstance(clazz);
            load(listener);
        } catch (Exception ex) {
            log.error("Failed to load listener", ex);
        }
    }


}
