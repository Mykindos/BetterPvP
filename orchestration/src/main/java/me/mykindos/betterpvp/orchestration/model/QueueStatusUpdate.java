package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueStatusUpdate(
        UUID playerUuid,
        String currentServer,
        QueueTarget queuedTarget,
        boolean active,
        boolean readyToConnect,
        int position,
        int queueSize,
        int effectivePriority,
        QueueState state,
        Long estimatedWaitSeconds,
        Instant enqueuedAt,
        long displayVersion
        ,
        String reservationId
) {

    public QueueStatusUpdate {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(currentServer, "currentServer");
        Objects.requireNonNull(queuedTarget, "queuedTarget");
        Objects.requireNonNull(state, "state");
        enqueuedAt = enqueuedAt == null ? Instant.now() : enqueuedAt;
    }

    public static QueueStatusUpdate cleared(UUID playerUuid, String currentServer, QueueTarget queuedTarget, long displayVersion) {
        return new QueueStatusUpdate(playerUuid, currentServer, queuedTarget, false, false, 0, 0, 0, QueueState.OPEN, null, Instant.now(), displayVersion, null);
    }
}
