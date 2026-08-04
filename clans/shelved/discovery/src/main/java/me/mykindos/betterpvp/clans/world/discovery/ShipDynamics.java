package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * The steering state of one ship: rudder, turn rate, heading and speed, advanced a tick at a time.
 * <p>
 * Headings are degrees in Minecraft yaw, so the unit vectors in the {@code (x, z)} plane are
 * <pre>
 * forward = (-sin(h),  cos(h))
 * right   = (-cos(h), -sin(h))
 * </pre>
 * Heading 0 therefore faces {@code +Z}, and the starboard side of a ship facing {@code +Z} is {@code -X}. A rising
 * heading is a turn to starboard, which is why a positive rudder produces a positive yaw rate.
 * <p>
 * Nothing here touches the world. The helm feeds it a time step and a net input, and reads back a heading and a speed
 * to apply — the motion itself is checkable in isolation.
 */
@Getter
public class ShipDynamics {

    private final ShipDynamicsConfig config;

    /** Wheel position, {@code -1} hard to port through {@code +1} hard to starboard. */
    private double rudder;

    /** Current rate of turn in degrees per second, lagging the rudder by {@link ShipDynamicsConfig#getHullResponse()}. */
    private double yawRate;

    /** Degrees, always normalised to {@code [0, 360)}. */
    private double heading;

    /** Blocks per second, falling as the rudder comes off centre. */
    private double speed;

    /** Multiplier on speed for sailing conditions. {@code 1.0} is a fair wind. */
    @Setter
    private double windFactor = 1.0;

    public ShipDynamics(@NotNull ShipDynamicsConfig config, double heading) {
        this.config = config;
        this.heading = normalise(heading);
        this.speed = config.getBaseSpeed();
    }

    /**
     * Steps the ship forward.
     *
     * @param dt       seconds elapsed
     * @param netInput {@code -1} to port, {@code +1} to starboard, {@code 0} for no input or both controls held
     */
    public void advance(double dt, int netInput) {
        if (netInput != 0) {
            final double resistance = 1.0 - config.getRudderResistance() * Math.abs(rudder);
            rudder += netInput * config.getRudderInRate() * dt * resistance;
        } else {
            final double decay = config.getRudderOutRate() * dt;
            // Stopping exactly at centre rather than stepping past it; an overshoot would flip the wheel's sign and
            // send the ship into a shallow weave with nobody touching the controls.
            rudder = rudder > 0 ? Math.max(0.0, rudder - decay) : Math.min(0.0, rudder + decay);
        }
        rudder = Math.clamp(rudder, -1.0, 1.0);

        // The lerp factor has to stay within [0, 1]: a long tick would otherwise step past the target turn rate and
        // ring instead of settling.
        final double response = Math.clamp(config.getHullResponse() * dt, 0.0, 1.0);
        yawRate += (config.getMaxYawRate() * rudder - yawRate) * response;

        heading = normalise(heading + yawRate * dt);
        speed = config.getBaseSpeed() * (1.0 - config.getTurnDrag() * Math.abs(rudder)) * windFactor;
    }

    public double forwardX() {
        return -Math.sin(Math.toRadians(heading));
    }

    public double forwardZ() {
        return Math.cos(Math.toRadians(heading));
    }

    public double rightX() {
        return -Math.cos(Math.toRadians(heading));
    }

    public double rightZ() {
        return -Math.sin(Math.toRadians(heading));
    }

    /** Folds any angle into {@code [0, 360)}. */
    public static double normalise(double degrees) {
        final double wrapped = degrees % 360.0;
        return wrapped < 0 ? wrapped + 360.0 : wrapped;
    }
}
