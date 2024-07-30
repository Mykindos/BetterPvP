package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.display.ClansSidebar;
import me.mykindos.betterpvp.clans.injector.ClansInjectorModule;
import me.mykindos.betterpvp.clans.leaderboards.ClansLeaderboardLoader;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.clans.tips.ClansTipLoader;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarController;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public class Clans extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    private ClanManager clanManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ClansInjectorModule(this),
                    new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:clans-migrations", "clans", TargetDatabase.LOCAL);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Clans"));

            var listenerLoader = injector.getInstance(ClansListenerLoader.class);
            listenerLoader.registerListeners(PACKAGE);

            var clansCommandLoader = injector.getInstance(ClansCommandLoader.class);
            clansCommandLoader.loadCommands(PACKAGE);

            var clanTipManager = injector.getInstance(ClansTipLoader.class);
            clanTipManager.loadTips(PACKAGE);

            clanManager = injector.getInstance(ClanManager.class);
            clanManager.loadFromList(clanManager.getRepository().getAll());

            var itemHandler = injector.getInstance(ItemHandler.class);
            itemHandler.loadItemData("clans");

            var uuidManager = injector.getInstance(UUIDManager.class);
            uuidManager.loadObjectsFromNamespace("clans");

            var clansSidebar = injector.getInstance(ClansSidebar.class);
            var sidebarController = injector.getInstance(SidebarController.class);
            if (clansSidebar.isEnabled()) {
                sidebarController.setDefaultProvider(gamer -> clansSidebar);
            }

            var leaderboardLoader = injector.getInstance(ClansLeaderboardLoader.class);
            leaderboardLoader.registerLeaderboards(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            final Adapters adapters = new Adapters(this);
            final Reflections reflectionAdapters = new Reflections(PACKAGE);
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));
        }
    }

    @Override
    public void onDisable() {
        clanManager.getRepository().processPropertyUpdates(false);
    }
}
