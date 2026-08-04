package me.mykindos.betterpvp.clans.world.discovery.wind;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The weather one ship is sailing in: what the wind is doing now, what it is turning into, and when it will be asked
 * for something new.
 * <p>
 * The factor eases toward its target rather than stepping onto it, because a ship that changed speed the instant the
 * weather did would read as a bug. Nothing here touches the world, so the whole progression is checkable in isolation.
 */
@Getter
public class Wind {

    private double factor = 1.0;

    private double target = 1.0;

    private WindBand band = WindBand.of(1.0);

    /** When the wind is next asked to become something else. Zero, so the first tick sets the weather. */
    private long nextShiftAt;

    public boolean due(long now) {
        return now >= nextShiftAt;
    }

    /** Sets the wind turning toward {@code target}, and puts the next change off by one period. */
    public void shift(double target, long now, long periodMillis) {
        this.target = target;
        this.nextShiftAt = now + Math.max(1L, periodMillis);
    }

    /**
     * Eases the factor toward its target.
     *
     * @param easeRate share of the remaining gap closed per second
     * @return the band the wind has just entered, or empty when it is still the one the crew were last told about
     */
    public @NotNull Optional<WindBand> advance(double dt, double easeRate) {
        factor += (target - factor) * Math.clamp(easeRate * dt, 0.0, 1.0);

        final WindBand reached = WindBand.of(factor);
        if (reached == band) {
            return Optional.empty();
        }
        band = reached;
        return Optional.of(reached);
    }
}
