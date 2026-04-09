package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JoinIntent(
        UUID playerUuid,
        String playerName,
        String currentServer,
        String requestedServer,
        Instant requestedAt
) {

    public JoinIntent {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(currentServer, "currentServer");
        Objects.requireNonNull(requestedServer, "requestedServer");
        requestedAt = requestedAt == null ? Instant.now() : requestedAt;
    }
}
