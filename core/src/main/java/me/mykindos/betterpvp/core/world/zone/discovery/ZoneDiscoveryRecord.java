package me.mykindos.betterpvp.core.world.zone.discovery;

import lombok.Value;

import java.time.LocalDateTime;

/**
 * A single persisted zone discovery, joined with its zone for display. Returned by
 * {@link ZoneDiscoveryRepository#listForClient(long)} and surfaced by the {@code /zone discovery list} command.
 */
@Value
public class ZoneDiscoveryRecord {

    String zoneKey;
    String displayName;
    LocalDateTime discoveredAt;
}
