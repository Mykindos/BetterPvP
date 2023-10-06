package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.injector.ClansInjectorModule;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public class Clans extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Inject
    @Config(path = "clans.database.prefix", defaultValue = "clans_")
    @Getter
    private String databasePrefix;

    private ClanManager clanManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ClansInjectorModule(this),
                    new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:clans-migrations", databasePrefix);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Clans"));

            var listenerLoader = injector.getInstance(ClansListenerLoader.class);
            listenerLoader.registerListeners(PACKAGE);

            var clansCommandLoader = injector.getInstance(ClansCommandLoader.class);
            clansCommandLoader.loadCommands(PACKAGE);

            clanManager = injector.getInstance(ClanManager.class);
            clanManager.loadFromList(clanManager.getRepository().getAll());

            updateEventExecutor.loadPlugin(this);

            var progression = Bukkit.getPluginManager().getPlugin("Progression");
            if (progression != null) {
                new ProgressionAdapter(this, (BPvPPlugin) progression, listenerLoader).load();
            }
        }
    }

    @Override
    public void onDisable() {
        clanManager.getRepository().processPropertyUpdates(false);
    }
}
