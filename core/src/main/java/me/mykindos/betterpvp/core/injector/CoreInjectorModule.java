package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.configuration.file.FileConfiguration;

public class CoreInjectorModule extends AbstractModule {

    private final Core plugin;

    public CoreInjectorModule(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Core.class).toInstance(plugin);
    }

}
