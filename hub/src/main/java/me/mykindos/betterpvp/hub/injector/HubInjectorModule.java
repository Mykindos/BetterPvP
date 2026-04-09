package me.mykindos.betterpvp.hub.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.sidebar.DefaultSidebarBuilder;
import me.mykindos.betterpvp.hub.feature.sidebar.HubSidebarBuilder;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.client.HttpOrchestrationGateway;

import java.net.URI;
import java.time.Duration;

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

    @Provides
    @Singleton
    public OrchestrationGateway provideOrchestrationGateway(Core core) {
        final String orchestrationBaseUrl = core.getConfig().getString("orchestration.base-url", "http://127.0.0.1:8085/");
        final long orchestrationRequestTimeoutMs = core.getConfig().getLong("orchestration.request-timeout-ms", 3000L);
        return new HttpOrchestrationGateway(
                URI.create(orchestrationBaseUrl),
                Duration.ofMillis(orchestrationRequestTimeoutMs)
        );
    }

}
