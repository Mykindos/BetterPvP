package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueEntryView(
        UUID playerUuid,
        String playerName,
        int position,
        int basePriority,
        int effectivePriority,
        Instant enqueuedAt
) {

    public QueueEntryView {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt");
    }
}
