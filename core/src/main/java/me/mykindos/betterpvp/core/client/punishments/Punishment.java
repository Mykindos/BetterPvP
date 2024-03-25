package me.mykindos.betterpvp.core.client.punishments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;

import java.util.UUID;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Punishment {

    private final UUID id;
    private final UUID client;
    private final IPunishmentType type;
    private final long expiryTime;
    private final String reason;
    private final String punisher;
    private boolean revoked;

    public boolean hasExpired() {
        return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
    }

    public boolean isActive() {
        return !revoked && !hasExpired();
    }

}
