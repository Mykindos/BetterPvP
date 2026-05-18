package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;
import me.mykindos.betterpvp.core.config.Config;

/**
 * Signals dying close to your own base. Pure distance: maximum at the clan
 * core, fading linearly to 0 at {@code homeRadiusBlocks} and beyond.
 * <p>
 * Death history is intentionally <b>not</b> considered here — the "repeated
 * deaths" angle is owned by {@link DeathFrequencyFactor} and
 * {@link DeathLocalityFactor}. This axis is purely positional, so the
 * {@code combine()} synergy multiplier (not this factor) is what makes
 * "near home" matter more when it coincides with rapid/clustered deaths.
 */
@Singleton
public class DistanceFromSafetyFactor implements FatigueFactor {

    @Inject
    @Config(path = "clans.fatigue.factor.distance.homeRadiusBlocks", defaultValue = "100.0")
    private double homeRadiusBlocks;

    @Inject
    @Config(path = "clans.fatigue.factor.distance.weight", defaultValue = "12.0")
    private double weight;

    @Override
    public String getName() {
        return "distanceFromSafety";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public double evaluate(BattleFatigue state, DeathContext context) {
        final double distance = context.distanceFromSafety();
        if (distance < 0) {
            return 0.0; // Unknown (no clan/core) — don't punish what we can't measure.
        }

        // Closer to home = stronger; zero once you're beyond the home radius.
        final double proximity = 1.0 - Math.min(1.0, distance / Math.max(1.0, homeRadiusBlocks));
        return Math.max(0.0, proximity);
    }
}
