package me.mykindos.betterpvp.core;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockModule;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.coretips.CoreTipLoader;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CurrentMode;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.injector.CoreInjectorModule;
import me.mykindos.betterpvp.core.inventory.InvUI;
import me.mykindos.betterpvp.core.leaderboards.CoreLeaderboardLoader;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.logging.LoggerFactory;
import me.mykindos.betterpvp.core.logging.appenders.DatabaseAppender;
import me.mykindos.betterpvp.core.logging.appenders.LegacyAppender;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.sound.SoundManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.OrientedVector;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@CustomLog
public class Core extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    private Database database;

    @Inject
    private Redis redis;

    private ClientManager clientManager;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Getter
    private ScoreboardLibrary scoreboardLibrary;

    @Getter
    @Setter
    private CurrentMode currentMode;

    @Getter
    @Setter
    private static int currentServer;

    @Getter
    @Setter
    private static String currentServerName;

    @Getter
    @Setter
    private static int currentSeason;

    @Getter
    @Setter
    private static int currentRealm;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(OrientedVector.class);


        // Add this appender first to ensure we still capture all logs before database is initialized
        LoggerFactory.getInstance().addAppender(new LegacyAppender());

        Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

        injector = Guice.createInjector(new CoreInjectorModule(this),
                new ConfigInjectorModule(this, fields),
                new SmartBlockModule());

        this.database = injector.getInstance(Database.class);
        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:core-migrations/postgres/", "global");

        setupServerAndSeason();
        injector.injectMembers(this);

        LoggerFactory.getInstance().addAppender(new DatabaseAppender(database, this));

        redis.credentials(this.getConfig());

        var coreListenerLoader = injector.getInstance(CoreListenerLoader.class);
        coreListenerLoader.registerListeners(PACKAGE);

        var coreCommandLoader = injector.getInstance(CoreCommandLoader.class);
        coreCommandLoader.loadCommands(PACKAGE);

        clientManager = injector.getInstance(ClientManager.class);

        this.saveConfig();

        var leaderboardLoader = injector.getInstance(CoreLeaderboardLoader.class);
        leaderboardLoader.registerLeaderboards(PACKAGE);

        var coreTipLoader = injector.getInstance(CoreTipLoader.class);
        coreTipLoader.loadTips(PACKAGE);

        var ruleManager = injector.getInstance(RuleManager.class);
        ruleManager.load(this);

        updateEventExecutor.loadPlugin(this);
        updateEventExecutor.initialize();

        InvUI.getInstance().setPlugin(this);

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            // If no packet adapter was found, you can fallback to the no-op implementation:
            scoreboardLibrary = new NoopScoreboardLibrary();
            log.warn("No packet adapter found, falling back to no-op implementation").submit();
        }

        final Adapters adapters = new Adapters(this);
        final Reflections reflectionAdapters = new Reflections(PACKAGE);
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

        UtilServer.runTaskLater(this, () -> UtilServer.callEvent(new ServerStartEvent()), 1L);
    }

    private void setupServerAndSeason() {
        if (Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
            String serverName = getCommonNameViaReflection();
            if (serverName == null) {
                serverName = "unknown";
            }

            if (serverName.toLowerCase().startsWith("champions")) {
                serverName = "Champions";
            }

            setCurrentServer(database.getServerId(serverName));
        } else {
            String serverName = getConfig().getOrSaveString("core.info.server", "unknown");
            setCurrentServerName(serverName);
            setCurrentServer(database.getServerId(serverName));
        }

        setCurrentSeason(getConfig().getOrSaveInt("core.info.season", 0));
        setCurrentRealm(database.getRealmId(getCurrentServer(), getCurrentSeason()));
    }

    @Override
    public void onDisable() {
        log.info("Shutting down...").submit();

        injector.getInstance(SoundManager.class).shutdown();
        log.info("Sound manager shut down").submit();

        injector.getInstance(SmartBlockDataManager.class).saveWorlds().join();
        log.info("Saved all cust0om blocks").submit();

        clientManager.processStatUpdates(false);
        clientManager.shutdown();
        log.info("Processed all pending stat updates and shut down client manager").submit();

        redis.shutdown();
        log.info("Redis connection closed").submit();

        injector.getInstance(GlobalCombatStatsRepository.class).shutdown();
        log.info("Global combat stats repository shut down").submit();

        scoreboardLibrary.close();
        log.info("Scoreboard library closed").submit();

        log.info("Closing logger factory...").submit();
        LoggerFactory.getInstance().close();
    }

    private String getCommonNameViaReflection() {
        try {
            Class<?> namespaceUtilClass = Class.forName("com.mineplex.studio.sdk.util.NamespaceUtil");
            java.lang.reflect.Method getCommonNameMethod = namespaceUtilClass.getMethod("getCommonName");
            return (String) getCommonNameMethod.invoke(null);
        } catch (Exception e) {
            log.error("Failed to get server common name", e).submit();
        }

        return "unknown";
    }
}