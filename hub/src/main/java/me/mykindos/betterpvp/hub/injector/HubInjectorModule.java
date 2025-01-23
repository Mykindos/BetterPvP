package me.mykindos.betterpvp.hub.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.sidebar.DefaultSidebarBuilder;
import me.mykindos.betterpvp.hub.feature.sidebar.HubSidebarBuilder;
import me.mykindos.betterpvp.hub.feature.sidebar.MineplexSidebarBuilder;

public class HubInjectorModule extends AbstractModule {

    private final Hub plugin;

    public HubInjectorModule(Hub plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Hub.class).toInstance(plugin);

        if (Compatibility.MINEPLEX) {
            bind(HubSidebarBuilder.class).toInstance(new MineplexSidebarBuilder());
        } else {
            bind(HubSidebarBuilder.class).to(DefaultSidebarBuilder.class);
        }
    }

}
