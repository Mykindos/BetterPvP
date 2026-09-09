package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * How long a crossing takes. A floor, a ceiling, and the odds on each roll in between.
 * <p>
 * The roll reads no clock and no random source. Given time at sea and a sample, it says whether land is sighted.
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
        // A ceiling below the floor lets the floor win, giving a fixed-length crossing.
        this.maxSeconds = Math.max(this.minSeconds, maxSeconds);
        this.chancePerRoll = Math.clamp(chancePerRoll, 0.0, 1.0);
    }

    /**
     * Whether the crew sights land on this roll.
     *
     * @param elapsedSeconds how long they have been at sea
     * @param sample         a value in {@code [0, 1)}
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

    /** Whether rolling has started. Holds back the arrival cues until the crossing is in doubt. */
    public boolean isRolling(long elapsedSeconds) {
        return elapsedSeconds >= minSeconds && elapsedSeconds < maxSeconds;
    }

    /**
     * Reads timing from config, falling back to {@link #DEFAULT} for anything unset.
     */
    public static @NotNull VoyageTiming of(int minSeconds, int maxSeconds, double chancePerRoll) {
        return new VoyageTiming(minSeconds, maxSeconds, chancePerRoll);
    }
}
