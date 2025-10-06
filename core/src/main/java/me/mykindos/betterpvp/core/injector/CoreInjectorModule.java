package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.filter.impl.DefaultFilterService;
import me.mykindos.betterpvp.core.chat.filter.impl.MineplexFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.chat.ignore.impl.DefaultIgnoreService;
import me.mykindos.betterpvp.core.chat.ignore.impl.MineplexIgnoreService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.database.MineplexDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.PostgresDatabaseConnection;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
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

        if (Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
            bind(IDatabaseConnection.class).to(MineplexDatabaseConnection.class);
            bind(IFilterService.class).to(MineplexFilterService.class);
            bind(IIgnoreService.class).to(MineplexIgnoreService.class);
            log.info("Using mineplex studio integrations").submit();
        } else {
            bind(IDatabaseConnection.class).to(PostgresDatabaseConnection.class);
            bind(IFilterService.class).to(DefaultFilterService.class);
            bind(IIgnoreService.class).to(DefaultIgnoreService.class);
            log.info("Using default integrations").submit();
        }

        bind(ClientManager.class).asEagerSingleton();
        bind(CooldownManager.class).asEagerSingleton();
        bind(EnergyHandler.class).asEagerSingleton();
        install(new CoreItemsModule());
    }

}
