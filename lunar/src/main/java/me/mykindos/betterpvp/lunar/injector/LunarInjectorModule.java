package me.mykindos.betterpvp.lunar.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.lunar.Lunar;

public class LunarInjectorModule extends AbstractModule {

    private final Lunar plugin;

    public LunarInjectorModule(Lunar plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Lunar.class).toInstance(plugin);
    }

}
