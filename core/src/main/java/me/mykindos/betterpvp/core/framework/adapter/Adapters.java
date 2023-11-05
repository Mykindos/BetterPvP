package me.mykindos.betterpvp.core.framework.adapter;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public final class Adapters {

    private final BPvPPlugin plugin;

    public Adapters(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAdapters(Collection<Class<?>> adapters) {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        for (Class<?> clazz : adapters) {
            if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()) || !clazz.isAnnotationPresent(PluginAdapter.class)) {
                continue;
            }

            PluginAdapter[] adapterAnnotation = clazz.getAnnotationsByType(PluginAdapter.class);
            if (adapterAnnotation.length == 0) {
                log.warn("Adapter " + clazz.getSimpleName() + " does not have a PluginAdapter annotation!");
                continue;
            }

            boolean pass = true;
            for (PluginAdapter annotation : adapterAnnotation) {
                if (pluginManager.getPlugin(annotation.value()) == null) {
                    log.warn("Plugin " + annotation.value() + " not found! Adapter " + clazz.getSimpleName() + " will not be loaded.");
                    pass = false;
                }
            }

            if (pass) {
                final String pluginName = Arrays.stream(adapterAnnotation).map(PluginAdapter::value).collect(Collectors.joining(", "));
                final Object adapter = plugin.getInjector().getInstance(clazz);
                plugin.getInjector().injectMembers(adapter);
                log.info("Loaded adapter " + clazz.getSimpleName() + " for " + pluginName + "!");
            }
        }
    }

}
