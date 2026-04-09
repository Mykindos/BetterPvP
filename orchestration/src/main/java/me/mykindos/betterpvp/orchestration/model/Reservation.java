package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Reservation(
        String reservationId,
        UUID playerUuid,
        QueueTarget target,
        Instant expiresAt,
        ReservationStatus status
) {

    public Reservation {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(status, "status");
    }
}
