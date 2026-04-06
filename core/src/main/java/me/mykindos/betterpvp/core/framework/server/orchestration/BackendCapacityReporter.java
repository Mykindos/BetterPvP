package me.mykindos.betterpvp.core.framework.server.orchestration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@BPvPListener
@Singleton
@CustomLog
public class BackendCapacityReporter implements Listener {

    private final Core core;
    private final ClientManager clientManager;
    private final OrchestrationGateway orchestrationGateway;
    private final AtomicBoolean orchestrationUnavailable = new AtomicBoolean(false);

    @Inject
    @Config(path = "orchestration.capacity-report.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "orchestration.capacity.soft-limit", defaultValue = "100")
    private int softCapacity;

    @Inject
    @Config(path = "orchestration.capacity.queue-state", defaultValue = "OPEN")
    private String queueState;

    @Inject
    public BackendCapacityReporter(Core core, ClientManager clientManager, OrchestrationGateway orchestrationGateway) {
        this.core = core;
        this.clientManager = clientManager;
        this.orchestrationGateway = orchestrationGateway;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        reportCapacityAsync();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        reportCapacityAsync();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reportCapacityAsync();
    }

    @UpdateEvent(delay = 2_000L, isAsync = true)
    public void reportTick() {
        reportCapacityAsync();
    }

    private void reportCapacityAsync() {
        if (!enabled) {
            return;
        }

        try {
            orchestrationGateway.updateCapacity(buildSnapshot()).join();
            if (orchestrationUnavailable.compareAndSet(true, false)) {
                log.info("Reconnected to orchestration service for backend capacity reporting").submit();
            }
        } catch (Exception ex) {
            if (orchestrationUnavailable.compareAndSet(false, true)) {
                log.warn("Failed to report backend capacity to orchestration service", ex).submit();
            }
        }
    }

    private ServerCapacitySnapshot buildSnapshot() {
        final int totalOnline = clientManager.getOnline().size();

        return new ServerCapacitySnapshot(
                Core.getCurrentRealm().getServer().getName(),
                parseQueueState(),
                Math.max(0, softCapacity),
                Math.max(0, totalOnline),
                0,
                0,
                Instant.now()
        );
    }

    private QueueState parseQueueState() {
        try {
            return QueueState.valueOf(queueState.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid orchestration queue state '{}', defaulting to OPEN", queueState).submit();
            return QueueState.OPEN;
        }
    }
}
