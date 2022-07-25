package me.mykindos.betterpvp.core;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.injector.DatabaseInjectorModule;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.UpdateEventExecutor;
import me.mykindos.betterpvp.core.injector.CoreInjectorModule;
import me.mykindos.betterpvp.core.listener.CoreListenerLoader;

public class Core extends BPvPPlugin {

    @Getter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    public void onEnable(){
        saveConfig();

        injector = Guice.createInjector(new CoreInjectorModule(this),
                new DatabaseInjectorModule());
        injector.injectMembers(this);

        database.getConnection().runDatabaseMigrations();

        CoreListenerLoader coreListenerLoader = new CoreListenerLoader(this);
        coreListenerLoader.registerListeners("me.mykindos.betterpvp.core");

        updateEventExecutor.initialize();
    }

}
