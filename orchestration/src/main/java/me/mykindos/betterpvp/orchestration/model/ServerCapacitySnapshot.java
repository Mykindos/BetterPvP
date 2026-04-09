package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;

public record ServerCapacitySnapshot(
        String serverName,
        QueueState state,
        int softCapacity,
        int regularOnline,
        int bypassOnline,
        int reservedRegular,
        Instant updatedAt
) {

    public ServerCapacitySnapshot {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(state, "state");
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public int availableRegularSlots() {
        return softCapacity - regularOnline - reservedRegular;
    }
}
