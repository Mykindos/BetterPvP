package me.mykindos.betterpvp.orchestration.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.json.OrchestrationObjectMapperFactory;
import me.mykindos.betterpvp.orchestration.model.AdmissionConfirmation;
import me.mykindos.betterpvp.orchestration.model.AdmissionDecision;
import me.mykindos.betterpvp.orchestration.model.DepartureNotification;
import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class HttpOrchestrationGateway implements OrchestrationGateway {

    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public HttpOrchestrationGateway(URI baseUri, Duration requestTimeout) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
        this.client = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .build();
        this.objectMapper = OrchestrationObjectMapperFactory.create();
    }

    @Override
    public CompletableFuture<AdmissionDecision> requestJoin(JoinIntent intent) {
        return sendJson("/api/v1/admission/join", "POST", intent, AdmissionDecision.class);
    }

    @Override
    public CompletableFuture<Void> leaveQueue(UUID playerUuid) {
        final HttpRequest request = HttpRequest.newBuilder(resolve("/api/v1/queue/player/" + playerUuid))
                .DELETE()
                .header("Accept", "application/json")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    ensureSuccess(response);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> updateCapacity(ServerCapacitySnapshot snapshot) {
        return sendJson("/api/v1/capacity", "POST", snapshot, Void.class);
    }

    @Override
    public CompletableFuture<Void> confirmArrival(AdmissionConfirmation confirmation) {
        return sendJson("/api/v1/admission/arrival", "POST", confirmation, Void.class);
    }

    @Override
    public CompletableFuture<Void> notifyDeparture(DepartureNotification notification) {
        return sendJson("/api/v1/admission/departure", "POST", notification, Void.class);
    }

    @Override
    public CompletableFuture<Void> upsertPlayerRank(PlayerRankSnapshot snapshot) {
        return sendJson("/api/v1/rank", "POST", snapshot, Void.class);
    }

    @Override
    public CompletableFuture<Void> removePlayerRank(UUID playerUuid) {
        final HttpRequest request = HttpRequest.newBuilder(resolve("/api/v1/rank/" + playerUuid))
                .DELETE()
                .header("Accept", "application/json")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    ensureSuccess(response);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Optional<PlayerRankSnapshot>> getPlayerRank(UUID playerUuid) {
        final HttpRequest request = HttpRequest.newBuilder(resolve("/api/v1/rank/" + playerUuid))
                .GET()
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        return Optional.empty();
                    }

                    ensureSuccess(response);
                    return Optional.of(readValue(response.body(), PlayerRankSnapshot.class));
                });
    }

    @Override
    public CompletableFuture<Optional<QueueStatusUpdate>> getPlayerQueueStatus(UUID playerUuid) {
        final HttpRequest request = HttpRequest.newBuilder(resolve("/api/v1/queue/player/" + playerUuid))
                .GET()
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        return Optional.empty();
                    }

                    ensureSuccess(response);
                    return Optional.of(readValue(response.body(), QueueStatusUpdate.class));
                });
    }

    @Override
    public CompletableFuture<QueueSnapshot> getQueueSnapshot(QueueTarget target) {
        final String query = "?targetId=" + encode(target.targetId())
                + "&targetType=" + encode(target.targetType().name())
                + "&serverName=" + encode(target.serverName());
        final HttpRequest request = HttpRequest.newBuilder(resolve("/api/v1/queue/target" + query))
                .GET()
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    ensureSuccess(response);
                    return readValue(response.body(), QueueSnapshot.class);
                });
    }

    @Override
    public CompletableFuture<Void> setQueueState(QueueTarget target, QueueState state) {
        return sendJson("/api/v1/queue/state", "POST", new QueueStateChangeRequest(target, state), Void.class);
    }

    @Override
    public CompletableFuture<Boolean> removeQueuedPlayer(UUID playerUuid) {
        return postBooleanAction("/api/v1/queue/remove", playerUuid);
    }

    @Override
    public CompletableFuture<Boolean> admitQueuedPlayer(UUID playerUuid) {
        return postBooleanAction("/api/v1/queue/admit", playerUuid);
    }

    private <T> CompletableFuture<T> sendJson(String path, String method, Object body, Class<T> responseType) {
        final byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }

        final HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .method(method, HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    ensureSuccess(response);
                    if (responseType == Void.class || response.statusCode() == 204 || response.body().isBlank()) {
                        return null;
                    }
                    return readValue(response.body(), responseType);
                });
    }

    private URI resolve(String path) {
        return baseUri.resolve(path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void ensureSuccess(HttpResponse<?> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        final Object body = response.body();
        final String bodyText = body == null ? "" : body.toString().trim();
        throw new CompletionException(new IllegalStateException(
                bodyText.isEmpty()
                        ? "Orchestration service request failed with status " + response.statusCode()
                        : "Orchestration service request failed with status " + response.statusCode() + ": " + bodyText
        ));
    }

    private <T> T readValue(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    private CompletableFuture<Boolean> postBooleanAction(String path, UUID playerUuid) {
        return sendJson(path, "POST", new PlayerActionRequest(playerUuid), BooleanActionResponse.class)
                .thenApply(BooleanActionResponse::success);
    }

    private record QueueStateChangeRequest(QueueTarget target, QueueState state) {
    }

    private record PlayerActionRequest(UUID playerUuid) {
    }

    private record BooleanActionResponse(boolean success) {
    }
}
