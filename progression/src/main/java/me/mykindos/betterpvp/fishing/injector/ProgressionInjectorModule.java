package me.mykindos.betterpvp.fishing.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.fishing.Progression;

public class ProgressionInjectorModule extends AbstractModule {

    private final Progression plugin;

    public ProgressionInjectorModule(Progression plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Progression.class).toInstance(plugin);
    }

}
