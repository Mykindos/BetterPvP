package me.mykindos.betterpvp.core.framework.adapter;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@CustomLog
public final class Adapters {

    private final BPvPPlugin plugin;

    public Adapters(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canLoad(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())) {
            return false;
        }

        PluginAdapter[] adapterAnnotation = clazz.getAnnotationsByType(PluginAdapter.class);
        for (PluginAdapter annotation : adapterAnnotation) {
            if (Bukkit.getPluginManager().getPlugin(annotation.value()) == null) {
                return false;
            }
        }

        return true;
    }

    public void loadAdapters(Collection<Class<?>> adapters) {
        for (Class<?> clazz : adapters) {
            if (!canLoad(clazz)) {
                log.warn("Could not load adapter " + clazz.getSimpleName() + "! Dependencies not found!").submit();
                continue;
            }

            final PluginAdapter[] adapterAnnotation = clazz.getAnnotationsByType(PluginAdapter.class);
            if (adapterAnnotation.length == 0) {
                continue;
            }

            final String pluginName = Arrays.stream(adapterAnnotation).map(PluginAdapter::value).collect(Collectors.joining(", "));
            final Object adapter = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(adapter);

            // Attempt to call the onLoad method
            try {
                clazz.getMethod(adapterAnnotation[0].loadMethodName()).invoke(adapter);
            } catch (IllegalAccessException e) {
                log.warn("Could not invoke load method for adapter " + clazz.getSimpleName() + " for " + pluginName + "!").submit();
            } catch (NoSuchMethodException ignored) {
                // Ignored because this is not required
            } catch (InvocationTargetException e) {
                log.error("Could not invoke load method for adapter " + clazz.getSimpleName() + " for " + pluginName + "!", e).submit();
                continue;
            }

            log.info("Loaded adapter " + clazz.getSimpleName() + " for " + pluginName + "!").submit();
        }
    }

}
