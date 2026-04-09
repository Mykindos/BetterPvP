package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DepartureNotification(
        UUID playerUuid,
        String serverName,
        boolean bypass,
        Instant occurredAt
) {

    public DepartureNotification {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(serverName, "serverName");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
