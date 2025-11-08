package me.mykindos.betterpvp.clans.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.clans.Clans;

public class ClansInjectorModule extends AbstractModule {

    private final Clans plugin;

    public ClansInjectorModule(Clans plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Clans.class).toInstance(plugin);
    }

}
