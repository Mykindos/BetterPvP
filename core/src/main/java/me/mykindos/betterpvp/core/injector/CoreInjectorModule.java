package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.filter.impl.DatabaseFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.chat.ignore.impl.DefaultIgnoreService;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.PostgresDatabaseConnection;
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


        bind(IDatabaseConnection.class).to(PostgresDatabaseConnection.class);
        bind(IFilterService.class).to(DatabaseFilterService.class);
        bind(IIgnoreService.class).to(DefaultIgnoreService.class);
        log.info("Using default integrations").submit();

    }

}
