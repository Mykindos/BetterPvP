package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import org.flywaydb.core.Flyway;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

@Singleton
@Slf4j
public class MySQLDatabaseConnection implements IDatabaseConnection {

    @Inject
    @Config(path = "core.database.ip", defaultValue = "127.0.0.1")
    private String sqlServer;

    @Inject
    @Config(path = "core.database.username")
    private String sqlUsername;

    @Inject
    @Config(path = "core.database.password")
    private String sqlPassword;

    @Inject
    @Config(path = "core.database.databaseName")
    private String sqlDatabaseName;

    private Connection connection;

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
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String prefix) {

        var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName;

        try {
            var flyway = Flyway.configure(classLoader)
                    .table(prefix + "schema_history")
                    .dataSource(url, sqlUsername, sqlPassword)
                    .locations(location)
                    .placeholders(Map.of("tablePrefix", prefix))
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)
                    .load();
            flyway.migrate();
        } catch (Exception ex) {
            log.error("Please correctly configure the MySQL database connection in the core plugin config.", ex);
        }

    }


}
