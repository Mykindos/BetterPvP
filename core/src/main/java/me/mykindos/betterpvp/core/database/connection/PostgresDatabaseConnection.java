package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.ConnectionData;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Singleton
@CustomLog
public class PostgresDatabaseConnection implements IDatabaseConnection {

    private final Core core;
    private ConnectionData dataSource;

    @Inject
    public PostgresDatabaseConnection(Core core) {
        this.core = core;
        configureHikari("core.database.global");
    }

    private void configureHikari(String configPath) {

        HikariConfig hikariConfig = new HikariConfig();

        var sqlServer = core.getConfig().getString(configPath + ".ip", "127.0.0.1");
        var sqlUsername = core.getConfig().getString(configPath + ".username");
        var sqlPassword = core.getConfig().getString(configPath + ".password");
        var sqlDatabaseName = core.getConfig().getString(configPath + ".databaseName");
        var maxPoolSize = core.getConfig().getInt(configPath + ".maxPoolSize");

        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + sqlServer + "/" + sqlDatabaseName);
        hikariConfig.setUsername(sqlUsername);
        hikariConfig.setPassword(sqlPassword);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.addDataSourceProperty("prepareThreshold", "3");
        hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "250");
        hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");

        ConnectionData connectionData = new ConnectionData(hikariConfig, new HikariDataSource(hikariConfig));
        dataSource = connectionData;
    }

    @Override
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String name) {

        try {
            var flyway = Flyway.configure(classLoader)
                    .table(name + "_schema_history")
                    .dataSource(dataSource.getDataSource())
                    .locations(location)
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)
                    .load();

            flyway.repair();
            flyway.migrate();
        } catch (Exception ex) {
            log.error("Please correctly configure the Postgres database connection in the core plugin config.", ex).submit();
        }

    }

    @Override
    public DataSource getDataSource() {
        return dataSource.getDataSource();
    }

}
