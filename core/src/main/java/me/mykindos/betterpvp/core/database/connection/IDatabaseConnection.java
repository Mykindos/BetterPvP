package me.mykindos.betterpvp.core.database.connection;

import java.sql.Connection;

public interface IDatabaseConnection {

    Connection getDatabaseConnection();

    void runDatabaseMigrations(ClassLoader classLoader, String location, String name);

}
