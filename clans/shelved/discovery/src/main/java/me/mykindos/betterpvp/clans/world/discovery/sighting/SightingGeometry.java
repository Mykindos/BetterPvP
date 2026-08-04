package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import org.jetbrains.annotations.NotNull;

/**
 * Where an island is drawn and how big it looks, worked out from the true offset alone.
 * <p>
 * A sighting is not drawn where it is. It is pinned at a fixed range along its true bearing, so it swings around the
 * horizon as the ship turns and never crawls closer — the distance text is what tells the crew they are closing, and
 * the icon growing over the last stretch is what tells them they are nearly there.
 */
public final class SightingGeometry {

    private SightingGeometry() {
    }

    /** The true bearing, taken out to exactly {@code pinDistance}. */
    public static @NotNull OceanOffset pinned(@NotNull OceanOffset trueOffset, double pinDistance) {
        final double distance = trueOffset.distance();
        if (distance <= 1e-9) {
            return new OceanOffset(0, pinDistance); // dead on top of the ship: dead ahead is as good a bearing as any
        }

        final double factor = pinDistance / distance;
        return new OceanOffset(trueOffset.getRight() * factor, trueOffset.getForward() * factor);
    }

    /**
     * How big the icon is drawn at a given true distance: {@code min} until the ramp starts, {@code max} from the
     * arrival distance in, and a straight line between them.
     */
    public static float scale(double trueDistance, double arrivalDistance, double rampStartDistance,
                              float min, float max) {
        if (rampStartDistance <= arrivalDistance) {
            return min; // a ramp with no room to run; growing on a divide by zero is worse than not growing
        }

        final double progress = Math.clamp(
                (rampStartDistance - trueDistance) / (rampStartDistance - arrivalDistance), 0.0, 1.0);
        return (float) (min + (max - min) * progress);
    }
}
