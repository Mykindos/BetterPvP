package me.mykindos.betterpvp.hub.feature.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class HubQueueStatusSyncListener implements Listener {

    private final ClientManager clientManager;
    private final HubQueueStatusRegistry queueStatusRegistry;
    private final OrchestrationGateway orchestrationGateway;

    @Inject
    public HubQueueStatusSyncListener(
            ClientManager clientManager,
            HubQueueStatusRegistry queueStatusRegistry,
            OrchestrationGateway orchestrationGateway
    ) {
        this.clientManager = clientManager;
        this.queueStatusRegistry = queueStatusRegistry;
        this.orchestrationGateway = orchestrationGateway;
    }

    @UpdateEvent(delay = 250L, isAsync = true)
    public void pollQueueStatuses() {
        clientManager.getOnline().forEach(client -> {
            try {
                orchestrationGateway.getPlayerQueueStatus(client.getUniqueId()).join()
                        .ifPresentOrElse(
                                queueStatusRegistry::applyUpdate,
                                () -> queueStatusRegistry.clear(client.getUniqueId())
                        );
            } catch (Exception ex) {
                log.warn("Failed syncing queue status for {}", client.getName(), ex).submit();
            }
        });
    }
}
