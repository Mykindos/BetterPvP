package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.manageddb.ManagedDBModule;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Singleton
@CustomLog
public final class MineplexDatabaseConnection implements IDatabaseConnection {

    private DataSource dataSource;

    @Inject
    private MineplexDatabaseConnection(Core core) {
        final ManagedDBModule module = MineplexModuleManager.getRegisteredModule(ManagedDBModule.class);
        configureMineplexDatabase(core, module, core.getConfig().getString("core.database.mineplex.globalDatabaseName"));
    }

    @SneakyThrows
    private void configureMineplexDatabase(Core core, ManagedDBModule module, String name) {

        final DataSource dataSource = new MineplexDataSource(core, module, name);
        this.dataSource = dataSource;
    }

    @Override
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String name) {
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

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }


}
