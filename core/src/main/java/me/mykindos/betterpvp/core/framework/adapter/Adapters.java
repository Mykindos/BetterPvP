package me.mykindos.betterpvp.core.framework.adapter;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import java.util.Collection;
import java.util.Optional;

@Slf4j
public final class Adapters {

    private final BPvPPlugin plugin;

    public Adapters(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAdapters(Collection<Class<?>> adapters) {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        for (Class<?> clazz : adapters) {
            PluginAdapter adapterAnnotation = clazz.getAnnotation(PluginAdapter.class);
            final String pluginName = adapterAnnotation.value();

            Optional.ofNullable(pluginManager.getPlugin(pluginName)).ifPresentOrElse(dependencyPlugin -> {
                try {
                    final Object adapter = plugin.getInjector().getInstance(clazz);
                    plugin.getInjector().injectMembers(adapter);
                    log.info("Loaded adapter " + clazz.getSimpleName() + " for " + pluginName + "!");
                } catch (Exception e) {
                    log.error("Failed to load adapter " + clazz.getSimpleName() + " for " + pluginName + "!", e);
                }
            }, () -> log.info("Plugin " + pluginName + " not found! Adapter " + clazz.getSimpleName() + " will not be loaded."));
        }
    }

}
