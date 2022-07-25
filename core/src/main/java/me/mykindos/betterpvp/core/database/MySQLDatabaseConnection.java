package me.mykindos.betterpvp.core.database;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.configuration.file.FileConfiguration;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Singleton
public class MySQLDatabaseConnection implements IDatabaseConnection {

    private final String sqlServer;
    private final String sqlUsername;
    private final String sqlPassword;
    private final String sqlDatabaseName;

    private Connection connection;

    @Inject
    public MySQLDatabaseConnection(Core core){
        FileConfiguration configuration = core.getConfig();
        sqlServer = configuration.getString("database.ip");
        sqlUsername = configuration.getString("database.username");
        sqlPassword = configuration.getString("database.password");
        sqlDatabaseName = configuration.getString("database.databaseName");
    }

    @Override
    public Connection getDatabaseConnection() {
        if(connection == null){
            var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName + "?autoReconnect=true&characterEncoding=latin1&useConfigs=maxPerformance";
            try {
                connection = DriverManager.getConnection(url, sqlUsername, sqlPassword);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return connection;
    }

}
