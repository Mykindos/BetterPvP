package me.mykindos.betterpvp.hub;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.hub.commands.HubCommandLoader;
import me.mykindos.betterpvp.hub.injector.HubInjectorModule;
import me.mykindos.betterpvp.hub.listener.HubListenerLoader;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public class Hub extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new HubInjectorModule(this),
                    new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Hub"));

            var listenerLoader = injector.getInstance(HubListenerLoader.class);
            listenerLoader.registerListeners(PACKAGE);

            var commandLoader = injector.getInstance(HubCommandLoader.class);
            commandLoader.loadCommands(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            final Adapters adapters = new Adapters(this);
            final Reflections reflectionAdapters = new Reflections(PACKAGE);
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));
        }
    }
}