package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * How often a hazard appears, and where it comes from.
 * <p>
 * Two shapes of spawn, because they ask different things of the helm. Something ahead has to be steered around and is
 * placed inside a narrow cone off the bow. Something on the flank arrives abeam and is a matter of noticing it, so it
 * comes in closer and to one side.
 * <p>
 * Randomness is supplied rather than drawn, so a whole run of spawns can be scripted in a test.
 */
@Value
public class HazardSpawnPolicy {

    /** Sensible pressure for a voyage that has not said otherwise. */
    public static final HazardSpawnPolicy DEFAULT = new HazardSpawnPolicy(0.3, 100, 25, 75, 0.3, 0.15);

    /** Expected spawns per second at depth zero. */
    double spawnsPerSecond;

    /** Range at which a hazard ahead is placed. */
    double aheadDistance;

    /** Half-width in degrees of the cone a hazard ahead is placed in. */
    double aheadBearingSpread;

    /** Range at which a hazard abeam is placed. */
    double flankDistance;

    /** Share of spawns that arrive abeam rather than ahead, in {@code [0, 1]}. */
    double flankChance;

    /** How much each unit of depth adds to the rate, as a fraction of {@link #spawnsPerSecond}. */
    double depthRateScale;

    /**
     * Rolls for a spawn over one step.
     *
     * @param depth  scales the rate up. Callers pass {@code 0} today; the knob exists so hazard density can be made to
     *               climb later without reshaping the policy.
     * @param dt     seconds elapsed
     * @param random draws in {@code [0, 1)}
     */
    public @NotNull Optional<HazardSpawn> next(int depth, double dt, @NotNull DoubleSupplier random) {
        final double rate = spawnsPerSecond * (1.0 + depth * depthRateScale);
        if (random.getAsDouble() >= rate * dt) {
            return Optional.empty();
        }

        final boolean flank = random.getAsDouble() < flankChance;
        final double side = random.getAsDouble();
        if (flank) {
            return Optional.of(new HazardSpawn(side < 0.5 ? -90 : 90, flankDistance));
        }
        return Optional.of(new HazardSpawn(aheadBearingSpread * (side * 2.0 - 1.0), aheadDistance));
    }
}
