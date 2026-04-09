package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PlayerRankSnapshot(
        UUID playerUuid,
        String playerName,
        String rank,
        String currentServer,
        Instant updatedAt
) {

    public PlayerRankSnapshot {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(currentServer, "currentServer");
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
