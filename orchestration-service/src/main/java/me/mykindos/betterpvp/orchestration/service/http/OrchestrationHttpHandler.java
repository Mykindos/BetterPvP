package me.mykindos.betterpvp.orchestration.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.mykindos.betterpvp.orchestration.model.AdmissionConfirmation;
import me.mykindos.betterpvp.orchestration.model.DepartureNotification;
import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.QueueTargetType;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;
import me.mykindos.betterpvp.orchestration.service.InMemoryOrchestrationGateway;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OrchestrationHttpHandler implements HttpHandler {

    private final InMemoryOrchestrationGateway gateway;
    private final JsonHttpResponder responder = new JsonHttpResponder();

    public OrchestrationHttpHandler(InMemoryOrchestrationGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (IllegalArgumentException ex) {
            logFailure(exchange, 400, ex);
            responder.writeError(exchange, 400, ex.getMessage());
        } catch (Exception ex) {
            logFailure(exchange, 500, ex);
            responder.writeError(exchange, 500, ex.getMessage() == null ? "Internal server error" : ex.getMessage());
        }
    }

    private void logFailure(HttpExchange exchange, int statusCode, Exception ex) {
        System.err.println("[orchestration] " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                + " -> " + statusCode + " " + ex.getClass().getSimpleName() + ": "
                + (ex.getMessage() == null ? "<no message>" : ex.getMessage()));
        ex.printStackTrace(System.err);
    }

    private void route(HttpExchange exchange) throws IOException {
        final String method = exchange.getRequestMethod();
        final String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && "/health".equals(path)) {
            responder.writeJson(exchange, 200, Map.of("status", "ok"));
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/admission/join".equals(path)) {
            final JoinIntent intent = responder.readBody(exchange, JoinIntent.class);
            responder.writeJson(exchange, 200, gateway.requestJoin(intent).join());
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/admission/arrival".equals(path)) {
            final AdmissionConfirmation confirmation = responder.readBody(exchange, AdmissionConfirmation.class);
            gateway.confirmArrival(confirmation).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/admission/departure".equals(path)) {
            final DepartureNotification notification = responder.readBody(exchange, DepartureNotification.class);
            gateway.notifyDeparture(notification).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/capacity".equals(path)) {
            final ServerCapacitySnapshot snapshot = responder.readBody(exchange, ServerCapacitySnapshot.class);
            gateway.updateCapacity(snapshot).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/rank".equals(path)) {
            final PlayerRankSnapshot snapshot = responder.readBody(exchange, PlayerRankSnapshot.class);
            gateway.upsertPlayerRank(snapshot).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        if (path.startsWith("/api/v1/rank/")) {
            handleRank(exchange, method, path.substring("/api/v1/rank/".length()));
            return;
        }

        if (path.startsWith("/api/v1/queue/player/")) {
            handlePlayerQueue(exchange, method, path.substring("/api/v1/queue/player/".length()));
            return;
        }

        if ("GET".equalsIgnoreCase(method) && "/api/v1/queue/target".equals(path)) {
            final QueueTarget target = readTarget(exchange.getRequestURI());
            responder.writeJson(exchange, 200, gateway.getQueueSnapshot(target).join());
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/queue/state".equals(path)) {
            final QueueStateChangeRequest request = responder.readBody(exchange, QueueStateChangeRequest.class);
            gateway.setQueueState(request.target(), request.state()).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/queue/remove".equals(path)) {
            final PlayerActionRequest request = responder.readBody(exchange, PlayerActionRequest.class);
            responder.writeJson(exchange, 200, new BooleanActionResponse(gateway.removeQueuedPlayer(request.playerUuid()).join()));
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/v1/queue/admit".equals(path)) {
            final PlayerActionRequest request = responder.readBody(exchange, PlayerActionRequest.class);
            responder.writeJson(exchange, 200, new BooleanActionResponse(gateway.admitQueuedPlayer(request.playerUuid()).join()));
            return;
        }

        responder.writeError(exchange, 404, "Route not found");
    }

    private void handleRank(HttpExchange exchange, String method, String rawPlayerId) throws IOException {
        final UUID playerUuid = UUID.fromString(rawPlayerId);

        if ("GET".equalsIgnoreCase(method)) {
            final Optional<?> rank = gateway.getPlayerRank(playerUuid).join();
            if (rank.isEmpty()) {
                responder.writeEmpty(exchange, 404);
                return;
            }

            responder.writeJson(exchange, 200, rank.get());
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            gateway.removePlayerRank(playerUuid).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        responder.writeError(exchange, 405, "Method not allowed");
    }

    private void handlePlayerQueue(HttpExchange exchange, String method, String rawPlayerId) throws IOException {
        final UUID playerUuid = UUID.fromString(rawPlayerId);

        if ("GET".equalsIgnoreCase(method)) {
            final Optional<?> status = gateway.getPlayerQueueStatus(playerUuid).join();
            if (status.isEmpty()) {
                responder.writeEmpty(exchange, 404);
                return;
            }

            responder.writeJson(exchange, 200, status.get());
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            gateway.leaveQueue(playerUuid).join();
            responder.writeEmpty(exchange, 204);
            return;
        }

        responder.writeError(exchange, 405, "Method not allowed");
    }

    private QueueTarget readTarget(URI uri) {
        final Map<String, String> params = QueryString.parse(uri.getRawQuery());
        final String targetId = required(params, "targetId");
        final QueueTargetType targetType = QueueTargetType.valueOf(required(params, "targetType"));
        final String serverName = required(params, "serverName");
        return new QueueTarget(targetId, targetType, serverName);
    }

    private String required(Map<String, String> params, String key) {
        final String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required query parameter: " + key);
        }
        return value;
    }

    private record QueueStateChangeRequest(QueueTarget target, QueueState state) {
    }

    private record PlayerActionRequest(UUID playerUuid) {
    }

    private record BooleanActionResponse(boolean success) {
    }
}
