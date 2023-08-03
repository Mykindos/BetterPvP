package me.mykindos.betterpvp.shops.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.shops.Shops;

public class ShopsInjectorModule extends AbstractModule {

    private final Shops plugin;

    public ShopsInjectorModule(Shops plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Shops.class).toInstance(plugin);
    }

}
