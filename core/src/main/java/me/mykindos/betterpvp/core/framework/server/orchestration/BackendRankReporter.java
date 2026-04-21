package me.mykindos.betterpvp.core.framework.server.orchestration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.events.ClientRankUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@BPvPListener
@Singleton
@CustomLog
public class BackendRankReporter implements Listener {

    private final ClientManager clientManager;
    private final OrchestrationGateway orchestrationGateway;
    private final AtomicBoolean orchestrationUnavailable = new AtomicBoolean(false);

    @Inject
    public BackendRankReporter(ClientManager clientManager, OrchestrationGateway orchestrationGateway) {
        this.clientManager = clientManager;
        this.orchestrationGateway = orchestrationGateway;
    }

    @EventHandler
    public void onClientLoad(AsyncClientLoadEvent event) {
        syncClient(event.getClient().getUniqueId().toString(), event.getClient().getName(), event.getClient().getRank().name());
    }

    @EventHandler
    public void onQuit(ClientQuitEvent event) {
        try {
            orchestrationGateway.removePlayerRank(event.getPlayer().getUniqueId()).join();
        } catch (Exception ex) {
            log.debug("Failed to remove rank snapshot for {}", event.getClient().getName(), ex).submit();
        }
    }

    @EventHandler
    public void onRankUpdate(ClientRankUpdateEvent event) {
        syncClient(event.getClient().getUniqueId().toString(), event.getClient().getName(), event.getNewRank().name());
    }

    @UpdateEvent(delay = 5_000L, isAsync = true)
    public void periodicSync() {
        clientManager.getOnline().forEach(client -> syncClient(client.getUniqueId().toString(), client.getName(), client.getRank().name()));
    }

    private void syncClient(String uuid, String playerName, String rank) {
        try {
            orchestrationGateway.upsertPlayerRank(new PlayerRankSnapshot(
                    java.util.UUID.fromString(uuid),
                    playerName,
                    rank,
                    Core.getCurrentRealm().getServer().getName(),
                    Instant.now()
            )).join();

            if (orchestrationUnavailable.compareAndSet(true, false)) {
                log.info("Reconnected to orchestration service for player rank reporting").submit();
            }
        } catch (Exception ex) {
            if (orchestrationUnavailable.compareAndSet(false, true)) {
                log.warn("Failed to sync player ranks to orchestration service", ex).submit();
            }
        }
    }
}
