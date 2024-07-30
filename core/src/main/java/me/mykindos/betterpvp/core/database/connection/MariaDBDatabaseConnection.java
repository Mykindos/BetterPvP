package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.ConnectionData;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.util.HashMap;

@Singleton
@CustomLog
public class MariaDBDatabaseConnection implements IDatabaseConnection {

    private final Core core;
    private final HashMap<TargetDatabase, ConnectionData> dataSources = new HashMap<>();

    @Inject
    public MariaDBDatabaseConnection(Core core) {
        this.core = core;
        configureHikari(TargetDatabase.LOCAL, "core.database.local");
        configureHikari(TargetDatabase.GLOBAL, "core.database.global");
    }

    private void configureHikari(TargetDatabase targetDatabase, String configPath) {

        if(dataSources.containsKey(targetDatabase)) {
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();

        var sqlServer = core.getConfig().getString(configPath + ".ip", "127.0.0.1");
        var sqlUsername = core.getConfig().getString(configPath + ".username");
        var sqlPassword = core.getConfig().getString(configPath + ".password");
        var sqlDatabaseName = core.getConfig().getString(configPath + ".databaseName");
        var maxPoolSize = core.getConfig().getInt(configPath + ".maxPoolSize");

        hikariConfig.setJdbcUrl("jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName);
        hikariConfig.setUsername(sqlUsername);
        hikariConfig.setPassword(sqlPassword);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        ConnectionData connectionData = new ConnectionData(hikariConfig, new HikariDataSource(hikariConfig));
        dataSources.put(targetDatabase, connectionData);
    }


    @SneakyThrows
    @Override
    public Connection getDatabaseConnection(TargetDatabase targetDatabase) {
        if(!dataSources.containsKey(targetDatabase)) {
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

        ConnectionData connectionData = dataSources.get(targetDatabase);
        if(connectionData == null) {
            log.error("Database connection not found for " + targetDatabase).submit();
            return;
        }

        try {
            var flyway = Flyway.configure(classLoader)
                    .table(name + "_schema_history")
                    .dataSource(connectionData.getDataSource())
                    .locations(location)
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)
                    .load();
            flyway.migrate();
        } catch (Exception ex) {
            log.error("Please correctly configure the MariaDB database connection in the core plugin config.", ex);
        }

    }


}
