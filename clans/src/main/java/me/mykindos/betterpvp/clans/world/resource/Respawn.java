package me.mykindos.betterpvp.clans.world.resource;

import org.jetbrains.annotations.NotNull;

import java.util.NavigableMap;
import java.util.Map;

/**
 * Pure respawn maths shared by every archetype, extracted from the old Fields system so it is unit-testable.
 * <p>
 * {@link #speedBuff} reproduces the player-count respawn multiplier; {@link #isReady} reproduces the
 * elapsed-since-last-harvest check. Neither touches Bukkit.
 */
public final class Respawn {

    private Respawn() {
    }

    /**
     * Resolves the respawn speed multiplier for the current online player count.
     *
     * @param bonusMultiplier an event/admin multiplier applied on top (1.0 = none)
     * @param playerCount     the number of online players
     * @param thresholds      player-count → multiplier, e.g. {@code {0:1.0, 5:1.5, 10:2.0}}; the highest threshold not
     *                        exceeding {@code playerCount} wins
     * @return {@code bonusMultiplier * matchedMultiplier}, or {@code bonusMultiplier} if no threshold matches
     */
    public static double speedBuff(double bonusMultiplier, int playerCount, @NotNull NavigableMap<Integer, Double> thresholds) {
        final Map.Entry<Integer, Double> entry = thresholds.floorEntry(playerCount);
        final double base = entry != null ? entry.getValue() : 1.0;
        return bonusMultiplier * base;
    }

    /**
     * @param lastUsedMs    when the node was last harvested (epoch millis)
     * @param delaySeconds  the base respawn delay in seconds
     * @param speedModifier the {@link #speedBuff} multiplier; values {@code <= 0} are treated as 1.0
     * @param nowMs         the current time (epoch millis)
     * @return true once enough time has elapsed for the node to respawn
     */
    public static boolean isReady(long lastUsedMs, double delaySeconds, double speedModifier, long nowMs) {
        final double modifier = speedModifier <= 0 ? 1.0 : speedModifier;
        final long requiredMs = (long) (delaySeconds * 1000.0 / modifier);
        return nowMs - lastUsedMs >= requiredMs;
    }
}
