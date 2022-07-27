package me.mykindos.betterpvp.core.database.connection;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import org.flywaydb.core.Flyway;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

@Singleton
public class MySQLDatabaseConnection implements IDatabaseConnection {

    @Inject
    @Config(path = "database.ip", defaultValue = "127.0.0.1")
    private String sqlServer;

    @Inject
    @Config(path = "database.username")
    private String sqlUsername;

    @Inject
    @Config(path = "database.password")
    private String sqlPassword;

    @Inject
    @Config(path = "database.databaseName")
    private String sqlDatabaseName;

    private Connection connection;

    @Override
    public Connection getDatabaseConnection() {

        if (connection == null) {
            var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName + "?autoReconnect=true&characterEncoding=latin1&useConfigs=maxPerformance";
            try {
                connection = DriverManager.getConnection(url, sqlUsername, sqlPassword);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return connection;
    }

    @Override
    public void runDatabaseMigrations(ClassLoader classLoader, String location, String prefix) {
        var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName;

        var flyway = Flyway.configure(classLoader)
                .dataSource(url, sqlUsername, sqlPassword)
                .locations(location)
                .placeholders(Map.of("tablePrefix", prefix))
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
        flyway.migrate();


    }


}
