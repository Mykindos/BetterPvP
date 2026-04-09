package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueTicket(
        String ticketId,
        UUID playerUuid,
        String currentServer,
        QueueTarget target,
        int basePriority,
        Instant enqueuedAt,
        Instant lastScoreUpdateAt,
        QueueState state
) {

    public QueueTicket {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(currentServer, "currentServer");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt");
        Objects.requireNonNull(lastScoreUpdateAt, "lastScoreUpdateAt");
        Objects.requireNonNull(state, "state");
    }
}
