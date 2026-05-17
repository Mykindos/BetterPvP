package me.mykindos.betterpvp.clans.clans.fatigue;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An immutable record of a single death, retained in a bounded history on
 * {@link BattleFatigue}. Factors read this history to derive recklessness.
 *
 * @param killer        the UUID of the killing player, or {@code null} for environmental deaths
 * @param location      where the death occurred
 * @param timestamp     epoch millis of the death
 */
public record DeathRecord(@Nullable UUID killer, Location location, long timestamp) {
}
