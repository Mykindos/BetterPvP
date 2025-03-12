package me.mykindos.betterpvp.game.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.guice.platform.DefaultPlatformProvider;
import me.mykindos.betterpvp.game.guice.platform.MineplexPlatformProvider;
import me.mykindos.betterpvp.game.guice.platform.PlatformProvider;
import me.mykindos.betterpvp.game.guice.provider.CurrentMapProvider;
import me.mykindos.betterpvp.game.guice.provider.WaitingLobbyProvider;
import org.bukkit.Bukkit;

@CustomLog
public class GlobalInjectorModule extends AbstractModule {

    private final GamePlugin plugin;

    public GlobalInjectorModule(GamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Bind plugin
        bind(GamePlugin.class).toInstance(plugin);

        // Set up GameScope - will exist throughout application but only active during IN_GAME
        GameScope gameScope = new GameScope();
        bind(GameScope.class).toInstance(gameScope);
        bindScope(GameScoped.class, gameScope);

        // Bind global services
        bind(ServerController.class).asEagerSingleton();
        bind(GameRegistry.class).asEagerSingleton();

        // Install platform-specific bindings
        PlatformProvider platformProvider;
        if (Compatibility.MINEPLEX) {
            platformProvider = new MineplexPlatformProvider();
        } else {
            platformProvider = new DefaultPlatformProvider();
        }

        install(platformProvider);
        log.info("Using {} platform provider", platformProvider.getPlatformName()).submit();

        // Bind map providers
        bind(MappedWorld.class).annotatedWith(Names.named("Waiting Lobby")).toProvider(WaitingLobbyProvider.class);
        bind(MappedWorld.class).annotatedWith(Names.named("Map")).toProvider(CurrentMapProvider.class);
    }
}