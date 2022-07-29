package me.mykindos.betterpvp.core;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.config.CoreConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.injector.DatabaseInjectorModule;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.injector.CoreInjectorModule;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;

public class Core extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    public void onEnable(){
        saveConfig();

        injector = Guice.createInjector(new CoreInjectorModule(this),
                new DatabaseInjectorModule(),
                new CoreConfigInjectorModule(this, PACKAGE));
        injector.injectMembers(this);

        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:core-migrations", "");

        var coreListenerLoader = injector.getInstance(CoreListenerLoader.class);
        coreListenerLoader.registerListeners(PACKAGE);

        var coreCommandLoader = injector.getInstance(CoreCommandLoader.class);
        coreCommandLoader.loadCommands(PACKAGE);

        updateEventExecutor.initialize();
    }

}
