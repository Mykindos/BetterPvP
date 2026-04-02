package me.mykindos.betterpvp.hub.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.sidebar.DefaultSidebarBuilder;
import me.mykindos.betterpvp.hub.feature.sidebar.HubSidebarBuilder;

public class HubInjectorModule extends AbstractModule {

    private final Hub plugin;

    public HubInjectorModule(Hub plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Hub.class).toInstance(plugin);
        bind(HubSidebarBuilder.class).to(DefaultSidebarBuilder.class);
    }

}
