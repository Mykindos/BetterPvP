package me.mykindos.betterpvp.core.listener.loader;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class ListenerLoader extends Loader {

    public ListenerLoader(BPvPPlugin plugin) {
        super(plugin);
    }

    public void load(Listener listener) {
        plugin.getInjector().injectMembers(listener);
        plugin.getListeners().add(listener);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        count++;
    }

    @Override
    public void load(Class<?> clazz) {
        try {
            Listener listener = (Listener) plugin.getInjector().getInstance(clazz);
            load(listener);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void reload(String packageName) {
        plugin.getListeners().forEach(listener -> {
            if (!listener.getClass().getPackageName().contains(packageName)) return;
            plugin.getInjector().injectMembers(listener);
        });
    }


}
