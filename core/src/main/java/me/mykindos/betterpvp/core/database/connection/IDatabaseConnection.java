package me.mykindos.betterpvp.core.database.connection;

import javax.sql.DataSource;

public interface IDatabaseConnection {

    void runDatabaseMigrations(ClassLoader classLoader, String location, String name);

    DataSource getDataSource();

}
