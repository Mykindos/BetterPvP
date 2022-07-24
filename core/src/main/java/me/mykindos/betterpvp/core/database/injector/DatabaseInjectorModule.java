package me.mykindos.betterpvp.core.database.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.database.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.MySQLDatabaseConnection;

public class DatabaseInjectorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IDatabaseConnection.class).to(MySQLDatabaseConnection.class);
    }

}
