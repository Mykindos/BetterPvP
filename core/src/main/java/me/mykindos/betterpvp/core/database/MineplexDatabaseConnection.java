package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.manageddb.ManagedDBModule;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;

@Singleton
@CustomLog
public final class MineplexDatabaseConnection implements IDatabaseConnection {

    private final HashMap<TargetDatabase, DataSource> dataSources = new HashMap<>();

    @Inject
    private MineplexDatabaseConnection(Core core) {
        final ManagedDBModule module = MineplexModuleManager.getRegisteredModule(ManagedDBModule.class);
        configureMineplexDatabase(core, module, TargetDatabase.GLOBAL, "global");
        configureMineplexDatabase(core, module, TargetDatabase.LOCAL, "local");
    }

    @SneakyThrows
    private void configureMineplexDatabase(Core core, ManagedDBModule module, TargetDatabase targetDatabase, String name) {
        if (dataSources.containsKey(targetDatabase)) {
            throw new RuntimeException("Database connection already exists for " + targetDatabase);
        }

        final DataSource dataSource = new MineplexDataSource(core, module, name);
        dataSources.put(targetDatabase, dataSource);
    }

    @SneakyThrows
    @Override
    public Connection getDatabaseConnection(TargetDatabase targetDatabase) {
        if (!dataSources.containsKey(targetDatabase)) {
            throw new RuntimeException("Database connection not found for " + targetDatabase);
        }

        return dataSources.get(targetDatabase).getConnection();
    }

    @SneakyThrows
    public Connection getDatabaseConnection() {
        return getDatabaseConnection(TargetDatabase.LOCAL);
    }

    @Override
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String name, TargetDatabase targetDatabase) {
        DataSource dataSource = dataSources.get(targetDatabase);
        if (dataSource == null) {
            log.error("DataSource not found for " + targetDatabase).submit();
            return;
        }

        try {
            var flyway = Flyway.configure(classLoader)
                    .table(name + "_schema_history")
                    .dataSource(dataSource)
                    .locations(location)
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)
                    .load();

            flyway.repair();
            flyway.migrate();
        } catch (Exception ex) {
            log.error("Please correctly configure the Mineplex database connection.", ex).submit();
        }

    }


}
