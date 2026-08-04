package me.mykindos.betterpvp.clans.world.voyage;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * How long a crossing takes: a floor, a ceiling, and the odds on each roll in between.
 * <p>
 * Three knobs rather than one because each answers a different complaint. The floor stops a voyage being over before
 * anyone has looked out at the water. The odds decide whether a place usually feels close or usually feels far. The
 * ceiling is the safety net — a flat per-roll chance has no upper bound, and the one crossing in forty that runs to
 * four minutes gets reported as a bug.
 * <p>
 * The roll itself is pure: given how long they have been sailing and a random sample, it says whether land is sighted.
 * Nothing here reads a clock or a random source, so the whole schedule is checkable.
 */
@Value
public class VoyageTiming {

    /** Sensible crossing for a destination that has not said otherwise. */
    public static final VoyageTiming DEFAULT = new VoyageTiming(45, 120, 0.25);

    /** Seconds before the first roll. No sighting can happen before this. */
    int minSeconds;

    /** Seconds after which arrival is certain, however the rolls have gone. */
    int maxSeconds;

    /** Chance each roll lands, once past the floor. */
    double chancePerRoll;

    public VoyageTiming(int minSeconds, int maxSeconds, double chancePerRoll) {
        this.minSeconds = Math.max(0, minSeconds);
        // A ceiling below the floor would make arrival both impossible and certain; the floor wins, giving a fixed-
        // length crossing rather than an undefined one.
        this.maxSeconds = Math.max(this.minSeconds, maxSeconds);
        this.chancePerRoll = Math.clamp(chancePerRoll, 0.0, 1.0);
    }

    /**
     * Whether the crew sights land on this roll.
     *
     * @param elapsedSeconds how long they have been at sea
     * @param sample         a value in {@code [0, 1)}; passed in rather than drawn here so the schedule can be tested
     */
    public boolean arrives(long elapsedSeconds, double sample) {
        if (elapsedSeconds >= maxSeconds) {
            return true;
        }
        if (elapsedSeconds < minSeconds) {
            return false;
        }
        return sample < chancePerRoll;
    }

    /** Whether rolling has started — used to hold back the "land ho" cues until the crossing is actually in doubt. */
    public boolean isRolling(long elapsedSeconds) {
        return elapsedSeconds >= minSeconds && elapsedSeconds < maxSeconds;
    }

    /**
     * Reads a destination's timing from config, falling back to {@link #DEFAULT} for anything unset — so a new island
     * needs no voyage block at all to behave sensibly.
     */
    public static @NotNull VoyageTiming of(int minSeconds, int maxSeconds, double chancePerRoll) {
        return new VoyageTiming(minSeconds, maxSeconds, chancePerRoll);
    }
}
