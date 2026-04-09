package me.mykindos.betterpvp.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.client.HttpOrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.AdmissionConfirmation;
import me.mykindos.betterpvp.orchestration.model.AdmissionDecision;
import me.mykindos.betterpvp.orchestration.model.AdmissionResult;
import me.mykindos.betterpvp.orchestration.model.DepartureNotification;
import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.transport.QueuePluginChannels;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QueueAdmissionListener {

    private static final MinecraftChannelIdentifier QUEUE_REQUEST_CHANNEL =
            MinecraftChannelIdentifier.from(QueuePluginChannels.QUEUE_REQUEST);

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ProxyConfig config;
    private final OrchestrationGateway orchestrationGateway;
    private final Map<UUID, QueueTarget> queuedTargetsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PendingAdmission> pendingAdmissionsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveManagedConnection> activeManagedConnections = new ConcurrentHashMap<>();
    private final Map<UUID, String> approvedTargetByPlayer = new ConcurrentHashMap<>();

    public QueueAdmissionListener(ProxyServer proxyServer, Logger logger, ProxyConfig config) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.config = config;
        this.orchestrationGateway = new HttpOrchestrationGateway(
                URI.create(config.orchestrationBaseUrl()),
                Duration.ofMillis(config.orchestrationRequestTimeoutMs())
        );
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        final RegisteredServer requested = event.getOriginalServer();
        final String serverName = requested.getServerInfo().getName();
        final Player player = event.getPlayer();
        if (consumeApprovedTarget(player.getUniqueId(), serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(requested));
            return;
        }

        final String currentServer = event.getPreviousServer() != null
                ? event.getPreviousServer().getServerInfo().getName()
                : player.getCurrentServer()
                    .map(connection -> connection.getServerInfo().getName())
                    .orElse("proxy");

        final AdmissionDecision decision = requestAdmission(player, currentServer, serverName);
        if (decision == null) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            return;
        }

        if (decision.result() == AdmissionResult.UNMANAGED) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(requested));
            return;
        }

        if (decision.result() == AdmissionResult.BYPASS || decision.result() == AdmissionResult.GRANTED) {
            logger.info("Admission {} for {} to {} reservation={}",
                    decision.result(), player.getUsername(), serverName, decision.reservationId());
            pendingAdmissionsByPlayer.put(player.getUniqueId(), new PendingAdmission(
                    decision.target(),
                    decision.reservationId(),
                    decision.result() == AdmissionResult.BYPASS
            ));
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(requested));
            return;
        }

        event.setResult(ServerPreConnectEvent.ServerResult.denied());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!QUEUE_REQUEST_CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection serverConnection)) {
            return;
        }

        final String requestedServer;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            requestedServer = input.readUTF();
        } catch (IOException ex) {
            logger.warn("Failed to read queue request plugin message", ex);
            return;
        }

        final Player player = serverConnection.getPlayer();
        final AdmissionDecision decision = requestAdmission(player, serverConnection.getServerInfo().getName(), requestedServer);
        if (decision == null) {
            return;
        }

        if (decision.result() == AdmissionResult.UNMANAGED) {
            final Optional<RegisteredServer> directTarget = proxyServer.getServer(requestedServer);
            if (directTarget.isEmpty()) {
                logger.warn("Requested unmanaged target {} for {} was unavailable", requestedServer, player.getUsername());
                return;
            }

            approveTarget(player.getUniqueId(), requestedServer);
            player.createConnectionRequest(directTarget.get()).fireAndForget();
            return;
        }

        if (decision.result() != AdmissionResult.BYPASS && decision.result() != AdmissionResult.GRANTED) {
            return;
        }

        final Optional<RegisteredServer> targetServer = proxyServer.getServer(requestedServer);
        if (targetServer.isEmpty()) {
            logger.warn("Managed target {} for {} was unavailable during plugin-message admission", requestedServer, player.getUsername());
            return;
        }

        pendingAdmissionsByPlayer.put(player.getUniqueId(), new PendingAdmission(
                decision.target(),
                decision.reservationId(),
                decision.result() == AdmissionResult.BYPASS
        ));
        logger.info("Plugin-message admission {} for {} to {} reservation={}",
                decision.result(), player.getUsername(), requestedServer, decision.reservationId());
        approveTarget(player.getUniqueId(), requestedServer);
        player.createConnectionRequest(targetServer.get()).fireAndForget();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String currentServerName = event.getServer().getServerInfo().getName();
        final ActiveManagedConnection previousManaged = activeManagedConnections.get(uuid);
        if (previousManaged != null && !previousManaged.target().serverName().equalsIgnoreCase(currentServerName)) {
            try {
                orchestrationGateway.notifyDeparture(new DepartureNotification(uuid, previousManaged.target().serverName(), previousManaged.bypass(), Instant.now())).join();
            } catch (Exception ex) {
                logger.debug("Failed notifying departure for {}", event.getPlayer().getUsername(), ex);
            }
            activeManagedConnections.remove(uuid);
        }

        final PendingAdmission pending = pendingAdmissionsByPlayer.remove(uuid);
        if (pending == null) {
            return;
        }

        if (!pending.target().serverName().equalsIgnoreCase(currentServerName)) {
            return;
        }

        try {
            orchestrationGateway.confirmArrival(new AdmissionConfirmation(uuid, currentServerName, pending.reservationId(), pending.bypass(), Instant.now())).join();
            logger.info("Confirmed arrival for {} on {} reservation={} bypass={}",
                    event.getPlayer().getUsername(), currentServerName, pending.reservationId(), pending.bypass());
        } catch (Exception ex) {
            logger.debug("Failed confirming arrival for {}", event.getPlayer().getUsername(), ex);
        }

        activeManagedConnections.put(uuid, new ActiveManagedConnection(pending.target(), pending.bypass()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        queuedTargetsByPlayer.remove(uuid);
        final ActiveManagedConnection activeManagedConnection = activeManagedConnections.remove(uuid);
        if (activeManagedConnection != null) {
            try {
                logger.info("Notifying departure for {} from {} bypass={}",
                        event.getPlayer().getUsername(), activeManagedConnection.target().serverName(), activeManagedConnection.bypass());
                orchestrationGateway.notifyDeparture(new DepartureNotification(uuid, activeManagedConnection.target().serverName(), activeManagedConnection.bypass(), Instant.now())).join();
            } catch (Exception ex) {
                logger.debug("Failed to notify departure for {} on disconnect", event.getPlayer().getUsername(), ex);
            }
        }
        try {
            orchestrationGateway.leaveQueue(uuid).join();
        } catch (Exception ex) {
            logger.debug("Failed to remove {} from queue on disconnect", event.getPlayer().getUsername(), ex);
        }
    }

    public void startPolling(Object plugin, ProxyServer proxyServer) {
        proxyServer.getScheduler()
                .buildTask(plugin, () -> pollQueuedPlayers(proxyServer))
                .repeat(Duration.ofSeconds(1))
                .schedule();
    }

    private void pollQueuedPlayers(ProxyServer proxyServer) {
        queuedTargetsByPlayer.forEach((uuid, target) -> {
            final Optional<Player> playerOptional = proxyServer.getPlayer(uuid);
            if (playerOptional.isEmpty()) {
                queuedTargetsByPlayer.remove(uuid);
                return;
            }

            final Player player = playerOptional.get();
            try {
                final Optional<QueueStatusUpdate> statusOptional = orchestrationGateway.getPlayerQueueStatus(uuid).join();
                if (statusOptional.isEmpty()) {
                    queuedTargetsByPlayer.remove(uuid);
                    return;
                }

                final QueueStatusUpdate status = statusOptional.get();
                if (!status.readyToConnect()) {
                    return;
                }
                logger.info("Queue ready for {} to {} reservation={}",
                        player.getUsername(), target.serverName(), status.reservationId());

                final Optional<RegisteredServer> targetServer = proxyServer.getServer(target.serverName());
                if (targetServer.isEmpty()) {
                    logger.warn("Managed target {} for {} was unavailable during queued dispatch", target.serverName(), player.getUsername());
                    return;
                }

                queuedTargetsByPlayer.remove(uuid);
                pendingAdmissionsByPlayer.put(uuid, new PendingAdmission(target, status.reservationId(), false));
                approveTarget(uuid, target.serverName());
                logger.info("Dispatching queued connection for {} to {} reservation={}",
                        player.getUsername(), target.serverName(), status.reservationId());
                player.createConnectionRequest(targetServer.get()).fireAndForget();
            } catch (Exception ex) {
                logger.debug("Failed polling queue status for {}", player.getUsername(), ex);
            }
        });
    }

    private AdmissionDecision requestAdmission(Player player, String currentServer, String serverName) {
        final JoinIntent intent = new JoinIntent(
                player.getUniqueId(),
                player.getUsername(),
                currentServer,
                serverName,
                Instant.now()
        );

        try {
            final AdmissionDecision decision = orchestrationGateway.requestJoin(intent).join();
            if (decision.result() == AdmissionResult.QUEUED) {
                queuedTargetsByPlayer.put(player.getUniqueId(), decision.target());
            } else if (decision.result() == AdmissionResult.DENIED || decision.result() == AdmissionResult.UNMANAGED) {
                queuedTargetsByPlayer.remove(player.getUniqueId());
            } else {
                queuedTargetsByPlayer.remove(player.getUniqueId());
            }
            return decision;
        } catch (Exception ex) {
            logger.warn("Failed handling queue admission for {}", player.getUsername(), ex);
            return null;
        }
    }

    private void approveTarget(UUID playerUuid, String serverName) {
        approvedTargetByPlayer.put(playerUuid, serverName.toLowerCase(java.util.Locale.ROOT));
    }

    private boolean consumeApprovedTarget(UUID playerUuid, String serverName) {
        return approvedTargetByPlayer.remove(playerUuid, serverName.toLowerCase(java.util.Locale.ROOT));
    }

    private record PendingAdmission(QueueTarget target, String reservationId, boolean bypass) {
    }

    private record ActiveManagedConnection(QueueTarget target, boolean bypass) {
    }
}
