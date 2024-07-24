package me.mykindos.betterpvp.progression;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
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
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.progression.commands.loader.ProgressionCommandLoader;
import me.mykindos.betterpvp.progression.injector.ProgressionInjectorModule;
import me.mykindos.betterpvp.progression.leaderboards.ProgressionLeaderboardLoader;
import me.mykindos.betterpvp.progression.listener.ProgressionListenerLoader;
import me.mykindos.betterpvp.progression.profession.fishing.repository.FishingRepository;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profile.repository.ProfessionProfileRepository;
import me.mykindos.betterpvp.progression.weapons.ProgressionWeaponManager;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@Singleton
public class Progression extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {
            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ProgressionInjectorModule(this), new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:progression-migrations", "progression");

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Progression"));

            var skillManager = injector.getInstance(ProgressionSkillManager.class);
            skillManager.loadSkills();

            var itemHandler = injector.getInstance(ItemHandler.class);
            itemHandler.loadItemData("progression");

            var listenerLoader = injector.getInstance(ProgressionListenerLoader.class);
            listenerLoader.registerListeners(PACKAGE);

            var commandLoader = injector.getInstance(ProgressionCommandLoader.class);
            commandLoader.loadCommands(PACKAGE);

            var leaderboardLoader = injector.getInstance(ProgressionLeaderboardLoader.class);
            leaderboardLoader.registerLeaderboards(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            injector.getInstance(ProgressionWeaponManager.class).load();

            final Adapters adapters = new Adapters(this);
            final Reflections reflectionAdapters = new Reflections(PACKAGE);
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

        }
    }

    @Override
    public void onDisable() {
        injector.getInstance(FishingRepository.class).saveAllFish(false);
        injector.getInstance(ProfessionProfileRepository.class).processStatUpdates(false);
    }

}