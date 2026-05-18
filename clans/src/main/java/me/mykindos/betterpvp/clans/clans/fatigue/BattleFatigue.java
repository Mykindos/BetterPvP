package me.mykindos.betterpvp.clans.clans.fatigue;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Per-player battle fatigue state. Pure domain object: it holds the score and a
 * bounded death history and knows how to prune itself, but contains no Bukkit
 * scheduling, no messaging, and no scoring policy. Decay and gain are driven
 * externally by {@link BattleFatigueManager}.
 */
@Getter
public class BattleFatigue {

    /** Hard ceiling so a streak can't run away past what the tiers expect. */
    public static final double MAX_SCORE = 100.0;

    /** How many recent deaths the factors are allowed to reason about. */
    private static final int MAX_HISTORY = 12;

    private final Deque<DeathRecord> recentDeaths = new ArrayDeque<>();

    @Setter
    private double score;

    /** Cached tier; recomputed by the manager whenever the score changes. */
    @Setter
    private FatigueTier tier = FatigueTier.FRESH;

    /** True while the player is trapped in the void-world respawn hold. */
    @Setter
    private boolean respawnHold;

    public void addScore(double delta) {
        this.score = Math.max(0.0, Math.min(MAX_SCORE, this.score + delta));
    }

    /**
     * Record a death, evicting the oldest entry once the history is full.
     */
    public void recordDeath(DeathRecord record) {
        recentDeaths.addLast(record);
        while (recentDeaths.size() > MAX_HISTORY) {
            recentDeaths.removeFirst();
        }
    }

    /**
     * Drop death records older than {@code windowMillis} so frequency/locality
     * factors only ever see the relevant window.
     */
    public void pruneOlderThan(long windowMillis) {
        final long cutoff = System.currentTimeMillis() - windowMillis;
        recentDeaths.removeIf(record -> record.timestamp() < cutoff);
    }

    /**
     * @return an unmodifiable view of the death history, oldest first.
     */
    public List<DeathRecord> getDeathHistory() {
        return List.copyOf(recentDeaths);
    }

    /**
     * @return true when the player has fully recovered and the entry can be evicted.
     */
    public boolean isIdle() {
        return score <= 0.0 && recentDeaths.isEmpty() && !respawnHold;
    }
}
