package me.mykindos.betterpvp.champions.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ChampionsItemBoostrap;

public class ChampionsInjectorModule extends AbstractModule {

    private final Champions plugin;

    public ChampionsInjectorModule(Champions plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Champions.class).toInstance(plugin);
        bind(ChampionsItemBoostrap.class).asEagerSingleton();
    }

}
