package me.mykindos.betterpvp.game;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CurrentMode;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.game.command.loader.GameCommandLoader;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.state.GameMapHandler;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.guice.GlobalInjectorModule;
import me.mykindos.betterpvp.game.guice.platform.PlatformProvider;
import me.mykindos.betterpvp.game.loader.GameListenerLoader;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
@CustomLog
public final class GamePlugin extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    private GameListenerLoader listenerLoader;

    private PlatformProvider platformProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core == null) {
            log.error("Core plugin not found!").submit();
            return;
        }

        core.setCurrentMode(CurrentMode.CHAMPIONS);

        Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

        injector = core.getInjector().createChildInjector(new GlobalInjectorModule(this), new ConfigInjectorModule(this, fields));
        injector.injectMembers(this);

        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:game-migrations/postgres", "game");
        Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Game"));

        var listenerLoader = injector.getInstance(GameListenerLoader.class);
        listenerLoader.registerListeners(PACKAGE);

        var shopsCommandLoader = injector.getInstance(GameCommandLoader.class);
        shopsCommandLoader.loadCommands(PACKAGE);

        final Adapters adapters = new Adapters(this);
        final Reflections reflectionAdapters = new Reflections(PACKAGE);
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

        updateEventExecutor.loadPlugin(this);

        platformProvider = injector.getInstance(PlatformProvider.class);
        platformProvider.setup();

        injector.getInstance(GameMapHandler.class).selectRandomGameAndMap();
        injector.getInstance(TransitionHandler.class).checkStateRequirements();

        injector.getInstance(ServerController.class).setAcceptingPlayers(true);
        log.info("Game is now accepting players").submit();
    }

    @Override
    public void onDisable() {
        injector.getInstance(MapManager.class).unload();
        platformProvider.tearDown();
    }
}