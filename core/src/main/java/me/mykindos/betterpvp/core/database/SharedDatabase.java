package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.MariaDBDatabaseConnection;

public class SharedDatabase extends Database {

    @Inject
    public SharedDatabase(Core core) {
        super(core, new MariaDBDatabaseConnection(
                core.getConfig().getString("core.database.global.ip", "127.0.0.1"),
                core.getConfig().getString("core.database.global.username"),
                core.getConfig().getString("core.database.global.password"),
                core.getConfig().getString("core.database.global.databaseName")
        ));
    }
}
