package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Singleton
@Slf4j
public class MariaDBDatabaseConnection implements IDatabaseConnection {

    private final String sqlServer;
    private final String sqlUsername;
    private final String sqlPassword;
    private final String sqlDatabaseName;
    private Connection connection;

    public MariaDBDatabaseConnection(String sqlServer, String sqlUsername, String sqlPassword, String sqlDatabaseName) {
        this.sqlServer = sqlServer;
        this.sqlUsername = sqlUsername;
        this.sqlPassword = sqlPassword;
        this.sqlDatabaseName = sqlDatabaseName;
    }

    public MariaDBDatabaseConnection(ExtendedYamlConfiguration config) {
        this.sqlServer = config.getString("core.database.local.ip", "127.0.0.1");
        this.sqlUsername = config.getString("core.database.local.username");
        this.sqlPassword = config.getString("core.database.local.password");
        this.sqlDatabaseName = config.getString("core.database.local.databaseName");
    }

    @Override
    public Connection getDatabaseConnection() {

        try {
            if (connection == null || connection.isClosed()) {
                var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName + "?autoReconnect=true&characterEncoding=latin1&useConfigs=maxPerformance";
                try {
                    connection = DriverManager.getConnection(url, sqlUsername, sqlPassword);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return connection;
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
