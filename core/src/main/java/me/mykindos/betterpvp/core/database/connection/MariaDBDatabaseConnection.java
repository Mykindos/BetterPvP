package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.flywaydb.core.Flyway;

import java.sql.Connection;

@Singleton
@CustomLog
public class MariaDBDatabaseConnection implements IDatabaseConnection {

    private final HikariConfig hikariConfig = new HikariConfig();
    private HikariDataSource dataSource;

    private final String sqlServer;
    private final String sqlUsername;
    private final String sqlPassword;
    private final String sqlDatabaseName;
    private final int maxPoolSize;

    public MariaDBDatabaseConnection(String sqlServer, String sqlUsername, String sqlPassword, String sqlDatabaseName, int maxPoolSize) {
        this.sqlServer = sqlServer;
        this.sqlUsername = sqlUsername;
        this.sqlPassword = sqlPassword;
        this.sqlDatabaseName = sqlDatabaseName;
        this.maxPoolSize = maxPoolSize;

        configureHikari();

    }

    public MariaDBDatabaseConnection(ExtendedYamlConfiguration config) {
        this.sqlServer = config.getString("core.database.local.ip", "127.0.0.1");
        this.sqlUsername = config.getString("core.database.local.username");
        this.sqlPassword = config.getString("core.database.local.password");
        this.sqlDatabaseName = config.getString("core.database.local.databaseName");
        this.maxPoolSize = config.getInt("core.database.local.maxPoolSize");

        configureHikari();
    }

    private void configureHikari() {
        hikariConfig.setJdbcUrl("jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName);
        hikariConfig.setUsername(sqlUsername);
        hikariConfig.setPassword(sqlPassword);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
    }


    @SneakyThrows
    public Connection getDatabaseConnection() {
        return dataSource.getConnection();
    }

    @Override
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String name) {

        var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName;

        try {
            var flyway = Flyway.configure(classLoader)
                    .table(name + "_schema_history")
                    .dataSource(url, sqlUsername, sqlPassword)
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
