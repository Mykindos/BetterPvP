package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.impl.chat.DefaultFilterService;
import me.mykindos.betterpvp.core.chat.IFilterService;
import me.mykindos.betterpvp.core.chat.impl.chat.MineplexFilterService;
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
            bind(IFilterService.class).to(MineplexFilterService.class);
            log.info("Using mineplex studio integrations").submit();
        } else {
            bind(IDatabaseConnection.class).to(MariaDBDatabaseConnection.class);
            bind(IFilterService.class).to(DefaultFilterService.class);
            log.info("Using default integrations").submit();
        }
    }

}
