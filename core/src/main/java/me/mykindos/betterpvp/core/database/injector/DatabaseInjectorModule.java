package me.mykindos.betterpvp.core.database.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.MySQLDatabaseConnection;

public class DatabaseInjectorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IDatabaseConnection.class).to(MySQLDatabaseConnection.class);
    }

}
