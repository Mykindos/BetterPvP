package me.mykindos.betterpvp.champions;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.champions.champions.leaderboards.ChampionsLeaderboardLoader;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.injector.SkillInjectorModule;
import me.mykindos.betterpvp.champions.commands.ChampionsCommandLoader;
import me.mykindos.betterpvp.champions.injector.ChampionsInjectorModule;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.champions.tips.ChampionsTipLoader;
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
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.recipes.RecipeHandler;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public class Champions extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    private ChampionsListenerLoader championsListenerLoader;

    @SneakyThrows
    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ChampionsInjectorModule(this),
                    new ConfigInjectorModule(this, fields),
                    new SkillInjectorModule(this));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:champions-migrations", "champions", TargetDatabase.LOCAL);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Champions"));

            var championsListenerLoader = injector.getInstance(ChampionsListenerLoader.class);
            championsListenerLoader.registerListeners(PACKAGE);

            var championsCommandLoader = injector.getInstance(ChampionsCommandLoader.class);
            championsCommandLoader.loadCommands(PACKAGE);

            var skillManager = injector.getInstance(ChampionsSkillManager.class);
            skillManager.loadSkills();

            var itemHandler = injector.getInstance(ItemHandler.class);
            itemHandler.loadItemData("champions");

            var recipeHandler = injector.getInstance(RecipeHandler.class);
            recipeHandler.loadConfig(this.getConfig(), "champions");

            var championsTipManager = injector.getInstance(ChampionsTipLoader.class);
            championsTipManager.loadTips(PACKAGE);

            var uuidManager = injector.getInstance(UUIDManager.class);
            uuidManager.loadObjectsFromNamespace("champions");

            var leaderboardLoader = injector.getInstance(ChampionsLeaderboardLoader.class);
            leaderboardLoader.registerLeaderboards(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            final Adapters adapters = new Adapters(this);
            final Reflections reflectionAdapters = new Reflections(PACKAGE);
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

            // We do this to force the static initializer to run, can be removed if we import this class anywhere
            Class.forName("me.mykindos.betterpvp.champions.effects.ChampionsEffectTypes");
        }
    }

}
