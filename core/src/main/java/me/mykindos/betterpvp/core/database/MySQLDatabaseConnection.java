package me.mykindos.betterpvp.core.database;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.FileUtil;
import org.flywaydb.core.Flyway;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


@Singleton
public class MySQLDatabaseConnection implements IDatabaseConnection {

    private final String sqlServer;
    private final String sqlUsername;
    private final String sqlPassword;
    private final String sqlDatabaseName;

    private Connection connection;
    private Core core;

    @Inject
    public MySQLDatabaseConnection(Core core) {
        this.core = core;
        FileConfiguration configuration = core.getConfig();
        sqlServer = configuration.getString("database.ip");
        sqlUsername = configuration.getString("database.username");
        sqlPassword = configuration.getString("database.password");
        sqlDatabaseName = configuration.getString("database.databaseName");
    }

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
    public void runDatabaseMigrations() {
        var url = "jdbc:mysql://" + sqlServer + "/" + sqlDatabaseName;

        var flyway = Flyway.configure(getClass().getClassLoader())
                .dataSource(url, sqlUsername, sqlPassword)
                .locations("classpath:database-migrations")
                .baselineOnMigrate(true)
                .validateMigrationNaming(true)
                .encoding("UTF-8")
                .sqlMigrationPrefix("V")
                .sqlMigrationSeparator("__")
                .sqlMigrationSuffixes(".sql")
                .load();
        flyway.migrate();


    }


}
