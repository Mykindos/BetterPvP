package me.mykindos.betterpvp.game.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.state.GameMapHandler;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.framework.listener.team.TeamBalancerListener;
import me.mykindos.betterpvp.game.framework.model.attribute.GlobalAttributeModule;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.guice.platform.DefaultPlatformProvider;
import me.mykindos.betterpvp.game.guice.platform.MineplexPlatformProvider;
import me.mykindos.betterpvp.game.guice.platform.PlatformProvider;
import me.mykindos.betterpvp.game.guice.provider.CurrentMapProvider;
import me.mykindos.betterpvp.game.guice.provider.WaitingLobbyProvider;

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

        // Bind map providers
        bind(MappedWorld.class).annotatedWith(Names.named("Waiting Lobby")).toProvider(WaitingLobbyProvider.class);
        bind(MappedWorld.class).annotatedWith(Names.named("Map")).toProvider(CurrentMapProvider.class);

        // Bind global services
        bind(ServerController.class).asEagerSingleton();
        bind(PlayerController.class).asEagerSingleton();
        bind(GameRegistry.class).asEagerSingleton();

        // Install game attribute module
        install(new GlobalAttributeModule());

        // These are bound in order of dependency. The rest don't have precedence
        bind(GameMapHandler.class).asEagerSingleton();
        bind(TransitionHandler.class).asEagerSingleton();
        bind(TeamBalancerListener.class).asEagerSingleton();

        // Install platform-specific bindings
        PlatformProvider platformProvider;
        if (Compatibility.MINEPLEX) {
            platformProvider = new MineplexPlatformProvider();
        } else {
            platformProvider = new DefaultPlatformProvider();
        }

        install(platformProvider);
        bind(PlatformProvider.class).toInstance(platformProvider);
        log.info("Using {} platform provider", platformProvider.getPlatformName()).submit();
    }
}
