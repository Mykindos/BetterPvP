package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import me.mykindos.betterpvp.clans.config.ClansConfigInjectorModule;
import me.mykindos.betterpvp.clans.injector.ClansInjectorModule;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.skills.SkillManager;
import me.mykindos.betterpvp.core.Core;

import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import org.bukkit.Bukkit;


public class Clans extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    @Config(path = "database.prefix", defaultValue = "clans_")
    private String databasePrefix;

    @Override
    public void onEnable() {

        saveConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            injector = core.getInjector().createChildInjector(new ClansInjectorModule(this),
                    new ClansConfigInjectorModule(this, PACKAGE));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:clans-migrations", databasePrefix);

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Clans"));

            var clansListenerLoader = injector.getInstance(ClansListenerLoader.class);
            clansListenerLoader.registerListeners(PACKAGE);

            var clansCommandLoader = injector.getInstance(ClansCommandLoader.class);
            clansCommandLoader.loadCommands(PACKAGE);

            var skillManager = injector.getInstance(SkillManager.class);
            skillManager.loadSkills();

        }
    }
}
