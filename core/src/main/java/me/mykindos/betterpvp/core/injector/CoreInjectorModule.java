package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.MineplexDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.MariaDBDatabaseConnection;
import org.bukkit.Bukkit;

@CustomLog
public class CoreInjectorModule extends AbstractModule {

    private final Core plugin;

    public CoreInjectorModule(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Core.class).toInstance(plugin);

        if(Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
            bind(IDatabaseConnection.class).to(MineplexDatabaseConnection.class);
            log.info("Found mineplex studio database integration").submit();
        } else {
            bind(IDatabaseConnection.class).to(MariaDBDatabaseConnection.class);
            log.info("No custom database connection loader found. Using default MariaDB connection.").submit();
        }
    }

}
