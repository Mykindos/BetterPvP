package me.mykindos.betterpvp.core.framework.server.orchestration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.client.HttpOrchestrationGateway;

import java.net.URI;
import java.time.Duration;

@Singleton
public class CoreOrchestrationGatewayProvider implements Provider<OrchestrationGateway> {

    @Inject
    private Core core;

    private volatile OrchestrationGateway gateway;

    @Override
    public OrchestrationGateway get() {
        OrchestrationGateway existing = gateway;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (gateway == null) {
                final String orchestrationBaseUrl = core.getConfig().getOrSaveString("orchestration.base-url", "http://127.0.0.1:8085/");
                final long orchestrationRequestTimeoutMs = core.getConfig().getOrSaveObject("orchestration.request-timeout-ms", 3000L, Long.class);
                gateway = new HttpOrchestrationGateway(
                        URI.create(orchestrationBaseUrl),
                        Duration.ofMillis(orchestrationRequestTimeoutMs)
                );
            }
            return gateway;
        }
    }
}
