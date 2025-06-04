package me.mykindos.betterpvp.champions;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.champions.achievements.loader.ChampionsAchievementCategoryLoader;
import me.mykindos.betterpvp.champions.achievements.loader.ChampionsAchievementLoader;
import me.mykindos.betterpvp.champions.champions.leaderboards.ChampionsLeaderboardLoader;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.injector.SkillInjectorModule;
import me.mykindos.betterpvp.champions.commands.ChampionsCommandLoader;
import me.mykindos.betterpvp.champions.injector.ChampionsInjectorModule;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponentSerializer;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.champions.tips.ChampionsTipLoader;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemLoader;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializationRegistry;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

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

            final Adapters adapters = new Adapters(this);
            final Reflections reflections = new Reflections(PACKAGE);
            final Reflections fieldReflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = fieldReflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ChampionsInjectorModule(this),
                    new ConfigInjectorModule(this, fields),
                    new SkillInjectorModule(this));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:champions-migrations/postgres/", "champions");

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Champions"));

            final ItemLoader itemLoader = new ItemLoader(this);
            registerSerializer();
            itemLoader.load(adapters, reflections.getTypesAnnotatedWith(ItemKey.class));

            var championsListenerLoader = injector.getInstance(ChampionsListenerLoader.class);
            championsListenerLoader.registerListeners(PACKAGE);

            var championsCommandLoader = injector.getInstance(ChampionsCommandLoader.class);
            championsCommandLoader.loadCommands(PACKAGE);

            var skillManager = injector.getInstance(ChampionsSkillManager.class);
            skillManager.loadSkills();

            var championsTipManager = injector.getInstance(ChampionsTipLoader.class);
            championsTipManager.loadTips(PACKAGE);

            var leaderboardLoader = injector.getInstance(ChampionsLeaderboardLoader.class);
            leaderboardLoader.registerLeaderboards(PACKAGE);

            var championsAchievementCategoryLoader = injector.getInstance(ChampionsAchievementCategoryLoader.class);
            championsAchievementCategoryLoader.loadAchievementCategories(PACKAGE);

            var championsAchievementLoader = injector.getInstance(ChampionsAchievementLoader.class);
            championsAchievementLoader.loadAchievements(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            var uuidManager = injector.getInstance(UUIDManager.class);
            uuidManager.loadObjectsFromNamespace("champions");

            adapters.loadAdapters(reflections.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflections.getTypesAnnotatedWith(PluginAdapters.class));

            // We do this to force the static initializer to run, can be removed if we import this class anywhere
            Class.forName("me.mykindos.betterpvp.champions.effects.ChampionsEffectTypes");
        }
    }

    private void registerSerializer() {
        injector.getInstance(ComponentSerializationRegistry.class).register(new ArmorStorageComponentSerializer());
    }

}
