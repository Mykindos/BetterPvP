package me.mykindos.betterpvp.lunar;

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
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.lunar.commands.loader.LunarCommandLoader;
import me.mykindos.betterpvp.lunar.injector.LunarInjectorModule;
import me.mykindos.betterpvp.lunar.listener.LunarListenerLoader;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public final class Lunar extends BPvPPlugin {


    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Inject
    @Config(path = "lunar.database.prefix", defaultValue = "lunar_")
    @Getter
    private String databasePrefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new LunarInjectorModule(this), new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:lunar-migrations", databasePrefix);
            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Lunar"));

            var shopsListenerLoader = injector.getInstance(LunarListenerLoader.class);
            shopsListenerLoader.registerListeners(PACKAGE);

            var shopsCommandLoader = injector.getInstance(LunarCommandLoader.class);
            shopsCommandLoader.loadCommands(PACKAGE);


            updateEventExecutor.loadPlugin(this);
        }
    }

}
