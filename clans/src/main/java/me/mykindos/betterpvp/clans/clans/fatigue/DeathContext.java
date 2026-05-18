package me.mykindos.betterpvp.clans.clans.fatigue;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Immutable snapshot of a death, assembled by {@link me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigueListener}
 * and handed to every {@link me.mykindos.betterpvp.clans.clans.fatigue.factor.FatigueFactor}.
 * <p>
 * Keeping this a pure value object (no Bukkit lookups inside factors) is what
 * makes the scoring layer unit-testable without a running server.
 *
 * @param victim             the player who died
 * @param killer             the killing player, or {@code null} for environmental / unknown deaths
 * @param location           where the death occurred
 * @param timestamp          epoch millis of the death
 * @param distanceFromSafety blocks from the victim to the nearest safety (own/closest wilderness);
 *                           {@code -1} if it could not be determined
 */
public record DeathContext(UUID victim,
                           @Nullable UUID killer,
                           Location location,
                           long timestamp,
                           double distanceFromSafety) {

    public DeathRecord toRecord() {
        return new DeathRecord(killer, location, timestamp);
    }
}
