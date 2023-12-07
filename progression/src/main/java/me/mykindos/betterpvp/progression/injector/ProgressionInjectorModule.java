package me.mykindos.betterpvp.progression.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.progression.Progression;

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
