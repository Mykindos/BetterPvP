package me.mykindos.betterpvp.orchestration.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AdmissionConfirmation(
        UUID playerUuid,
        String serverName,
        String reservationId,
        boolean bypass,
        Instant confirmedAt
) {

    public AdmissionConfirmation {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(serverName, "serverName");
        confirmedAt = confirmedAt == null ? Instant.now() : confirmedAt;
    }
}
