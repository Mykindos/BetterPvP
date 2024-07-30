package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.MariaDBDatabaseConnection;
import org.bukkit.Bukkit;

public class CoreInjectorModule extends AbstractModule {

    private final Core plugin;

    public CoreInjectorModule(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Core.class).toInstance(plugin);

        if(Bukkit.getPluginManager().getPlugin("MineplexSDK") != null) { // Fix names
            // TODO create mineplex database impl
        } else {
            bind(IDatabaseConnection.class).to(MariaDBDatabaseConnection.class);
        }
    }

}
