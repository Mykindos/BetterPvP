package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.MariaDBDatabaseConnection;
import org.reflections.Reflections;

import java.util.Set;

@CustomLog
public class CoreInjectorModule extends AbstractModule {

    private final Core plugin;

    public CoreInjectorModule(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Core.class).toInstance(plugin);

        final Set<Class<? extends IDatabaseConnection>> found = new Reflections("me.mykindos").getSubTypesOf(IDatabaseConnection.class);
        found.remove(MariaDBDatabaseConnection.class);
        if (found.isEmpty()) {
            bind(IDatabaseConnection.class).to(MariaDBDatabaseConnection.class);
            log.info("No custom database connection loader found. Using default MariaDB connection.").submit();
        } else {
            final Class<? extends IDatabaseConnection> connectionLoader = found.iterator().next();
            bind(IDatabaseConnection.class).to(connectionLoader);
            log.info("Found custom database connection loader: " + connectionLoader.getName()).submit();
        }
    }

}
