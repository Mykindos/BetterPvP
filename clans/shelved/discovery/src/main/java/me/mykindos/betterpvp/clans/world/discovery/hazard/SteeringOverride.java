package me.mykindos.betterpvp.clans.world.discovery.hazard;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.discovery.ShipDynamics;

/**
 * Something other than the crew with a hand on the wheel, for a while.
 * <p>
 * It works by replacing the net input {@link ShipDynamics#advance} is given rather than by writing to the rudder, and
 * that is the whole trick: the wheel still has to be wound over against its own resistance curve, so the ship leans
 * into the turn over about a second instead of snapping to hard-over on one tick. Nothing has to model the ramp
 * because the rudder already does.
 * <p>
 * Pure state, so a whole seizure can be run through in a test at whatever clock the test likes.
 */
@Getter
public class SteeringOverride {

    /** {@code -1} to port, {@code +1} to starboard, {@code 0} when the crew has the wheel back. */
    private int side;

    /** When the grip lets go, in the same milliseconds the caller ticks with. */
    private long until;

    /** Takes the wheel over to one side and holds it there. */
    public void seize(int side, long now, long durationMillis) {
        this.side = Integer.signum(side);
        this.until = now + durationMillis;
    }

    public boolean isActive(long now) {
        return side != 0 && now < until;
    }

    /** Milliseconds of grip left, zero once it has lapsed. */
    public long remaining(long now) {
        return isActive(now) ? until - now : 0L;
    }

    /**
     * The input the ship actually gets: the override's side while it holds, and the crew's own the moment it does not.
     * <p>
     * A lapsed grip clears itself here rather than needing anything to sweep it, so the tick that hands control back is
     * the same tick that notices it expired.
     */
    public int apply(int crewInput, long now) {
        if (!isActive(now)) {
            side = 0;
            return crewInput;
        }
        return side;
    }

    /** Hands the wheel back immediately. */
    public void release() {
        side = 0;
        until = 0L;
    }
}
