package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;

/**
 * Where something sits relative to the ship, in the ship's own frame: how far to starboard and how far ahead.
 * <p>
 * This is the form everything downstream wants — a hazard is placed off the bow, a landfall is announced off the beam —
 * so the ocean hands out offsets rather than world coordinates.
 */
@Value
public class OceanOffset {

    /** Blocks to starboard. Negative is to port. */
    double right;

    /** Blocks ahead. Negative is astern. */
    double forward;

    /** Straight-line distance from the ship. */
    public double distance() {
        return Math.hypot(right, forward);
    }

    /** Degrees off the bow: {@code 0} dead ahead, {@code +90} abeam to starboard, in {@code (-180, 180]}. */
    public double bearing() {
        return Math.toDegrees(Math.atan2(right, forward));
    }
}
