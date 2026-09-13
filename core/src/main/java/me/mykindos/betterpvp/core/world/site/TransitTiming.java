package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * How long travel to a site takes. A floor, a ceiling, and the odds on each roll in between.
 * <p>
 * The roll reads no clock and no random source. Given an elapsed time and a sample, it says whether to arrive, which
 * is what makes it testable without waiting.
 */
@Value
public class TransitTiming {

    /** What a site that has not said otherwise takes. */
    public static final TransitTiming DEFAULT = new TransitTiming(45, 120, 0.25);

    /** Seconds before the first roll. Nothing can arrive before this. */
    int minSeconds;

    /** Seconds after which arrival happens regardless of how the rolls went. */
    int maxSeconds;

    /** Chance each roll lands, once past the floor. */
    double chancePerRoll;

    public TransitTiming(int minSeconds, int maxSeconds, double chancePerRoll) {
        this.minSeconds = Math.max(0, minSeconds);
        // A ceiling below the floor lets the floor win, giving a fixed-length journey.
        this.maxSeconds = Math.max(this.minSeconds, maxSeconds);
        this.chancePerRoll = Math.clamp(chancePerRoll, 0.0, 1.0);
    }

    /**
     * Whether this roll arrives.
     *
     * @param elapsedSeconds how long the journey has been running
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
    public static @NotNull TransitTiming of(int minSeconds, int maxSeconds, double chancePerRoll) {
        return new TransitTiming(minSeconds, maxSeconds, chancePerRoll);
    }
}
