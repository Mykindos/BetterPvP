package me.mykindos.betterpvp.clans.clans.fatigue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.fatigue.events.PlayerFatigueGainEvent;
import me.mykindos.betterpvp.clans.clans.fatigue.events.PlayerFatigueTierChangeEvent;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.FatigueFactor;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;

/**
 * Owns battle-fatigue scoring: it sums the registered {@link FatigueFactor}
 * strategies into a single recklessness gain, applies passive time decay, and
 * resolves the {@link FatigueTier}. It fires events on change and knows nothing
 * about slowness, titles, or the void world — those concerns live elsewhere.
 * <p>
 * State is session-only: entries live in the in-memory map and are evicted once
 * a player has fully recovered or logged off.
 */
@Singleton
@BPvPListener
@CustomLog
public class BattleFatigueManager extends Manager<String, BattleFatigue> implements Listener {

    private final Set<FatigueFactor> factors;

    @Inject
    @Config(path = "clans.fatigue.decayPerSecond", defaultValue = "0.8")
    private double decayPerSecond;

    @Inject
    @Config(path = "clans.fatigue.deathHistoryWindowSeconds", defaultValue = "60")
    private long deathHistoryWindowSeconds;

    @Inject
    @Config(path = "clans.fatigue.threshold.worn", defaultValue = "35.0")
    private double thresholdWorn;

    @Inject
    @Config(path = "clans.fatigue.threshold.weary", defaultValue = "65.0")
    private double thresholdWeary;

    @Inject
    @Config(path = "clans.fatigue.threshold.exhausted", defaultValue = "90.0")
    private double thresholdExhausted;

    // --- combine() formula tunables (global to the algorithm, not per-factor) -------
    @Inject
    @Config(path = "clans.fatigue.combine.baseGain", defaultValue = "2.0")
    private double baseGain;

    /** A signal at/above this counts as an "active axis" for synergy. */
    @Inject
    @Config(path = "clans.fatigue.combine.synergy.activationThreshold", defaultValue = "0.35")
    private double synergyActivationThreshold;

    /** Each active axis beyond the first multiplies the weighted sum by this much more. */
    @Inject
    @Config(path = "clans.fatigue.combine.synergy.perExtraAxis", defaultValue = "0.55")
    private double synergyPerExtraAxis;

    /** Hard cap on a single death's gain, so one death can't skip a whole tier. */
    @Inject
    @Config(path = "clans.fatigue.combine.maxGainPerDeath", defaultValue = "28.0")
    private double maxGainPerDeath;

    @Inject
    public BattleFatigueManager(Set<FatigueFactor> factors) {
        this.factors = factors;
    }

    public BattleFatigue getOrCreate(UUID uuid) {
        return objects.computeIfAbsent(uuid.toString(), key -> new BattleFatigue());
    }

    public FatigueTier getTier(UUID uuid) {
        return getObject(uuid).map(BattleFatigue::getTier).orElse(FatigueTier.FRESH);
    }

    /**
     * Process a death: evaluate every factor, fold them into a gain, update the
     * score and tier, and fire the appropriate event. Returns the resolved tier
     * so the caller (the listener) can decide whether to trigger the hold.
     */
    public FatigueTier recordDeath(Player player, DeathContext context) {
        final BattleFatigue state = getOrCreate(player.getUniqueId());
        state.pruneOlderThan(deathHistoryWindowSeconds * 1000L);

        final double previousScore = state.getScore();
        final FatigueTier previousTier = state.getTier();

        final double gain = computeGain(state, context);
        state.addScore(gain);
        state.recordDeath(context.toRecord());

        final FatigueTier newTier = resolveTier(state.getScore());
        state.setTier(newTier);

        Bukkit.broadcastMessage("Score for " + player.getName() + ": " + state.getScore());

        if (newTier != previousTier) {
            UtilServer.callEvent(new PlayerFatigueTierChangeEvent(player, previousTier, newTier));
        } else {
            UtilServer.callEvent(new PlayerFatigueGainEvent(player, previousScore, state.getScore()));
        }
        return newTier;
    }

    /**
     * Fold every registered factor into a single gain. Fully generic: it asks
     * each {@link FatigueFactor} for its normalized signal and its weight and
     * never names or special-cases a concrete factor. Adding a factor changes
     * nothing here.
     * <p>
     * The bad-vs-reckless line is the <b>synergy multiplier</b>: a merely-bad
     * player spikes ONE axis and gets no multiplier (passive decay erases it);
     * a reckless player lights up several axes at once and the multiplier makes
     * it bite. {@code baseGain} is a per-death floor; the result is capped so a
     * single death can't leap a whole tier. Score is further clamped to
     * {@code [0, 100]} by {@link BattleFatigue#addScore}.
     *
     * @param state   the victim's current fatigue state (history pre-this-death)
     * @param context the death snapshot
     * @return points to add to the fatigue score
     */
    private double computeGain(BattleFatigue state, DeathContext context) {
        double weightedSum = 0.0;
        int activeAxes = 0;

        for (FatigueFactor factor : factors) {
            // Defensive clamp: honor the [0,1] contract even if a factor misbehaves.
            final double signal = Math.max(0.0, Math.min(1.0, factor.evaluate(state, context)));
            weightedSum += factor.getWeight() * signal;
            if (signal >= synergyActivationThreshold) {
                activeAxes++;
            }
        }

        final double synergyMultiplier = 1.0 + synergyPerExtraAxis * Math.max(0, activeAxes - 1);
        final double gain = baseGain + weightedSum * synergyMultiplier;
        return Math.min(gain, maxGainPerDeath);
    }

    private FatigueTier resolveTier(double score) {
        if (score >= thresholdExhausted) {
            return FatigueTier.EXHAUSTED;
        }
        if (score >= thresholdWeary) {
            return FatigueTier.WEARY;
        }
        if (score >= thresholdWorn) {
            return FatigueTier.WORN;
        }
        return FatigueTier.FRESH;
    }

    /**
     * Passive recovery. Runs once a second, bleeding the score down and emitting
     * a tier-change event if a player drops back into a calmer band.
     */
    @UpdateEvent(delay = 1000)
    public void decay() {
        objects.entrySet().removeIf(entry -> {
            final BattleFatigue state = entry.getValue();
            if (state.isRespawnHold()) {
                return false; // never decay or evict while mid-hold
            }

            if (state.getScore() > 0.0) {
                final FatigueTier before = state.getTier();
                state.addScore(-decayPerSecond);
                final FatigueTier after = resolveTier(state.getScore());
                if (after != before) {
                    state.setTier(after);
                    final Player player = Bukkit.getPlayer(UUID.fromString(entry.getKey()));
                    if (player != null) {
                        UtilServer.callEvent(new PlayerFatigueTierChangeEvent(player, before, after));
                    }
                }
            }

            state.pruneOlderThan(deathHistoryWindowSeconds * 1000L);
            return state.isIdle();
        });
    }
}
