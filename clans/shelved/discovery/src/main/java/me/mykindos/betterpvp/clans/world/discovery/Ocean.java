package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The ship's position on an unbounded virtual sea, and the transform between that plane and the ship's own frame.
 * <p>
 * The sea has no map behind it: only the ship moves, and everything else is remembered as a fixed
 * {@link OceanPoint} that the ship sails past. Headings follow the Minecraft-yaw convention documented on
 * {@link ShipDynamics}.
 */
@Getter
public class Ocean {

    private double x;

    private double z;

    public Ocean(double x, double z) {
        this.x = x;
        this.z = z;
    }

    /** Slides the ship along its heading for one step. */
    public void advance(double dt, double speed, double heading) {
        final double radians = Math.toRadians(heading);
        x += -Math.sin(radians) * speed * dt;
        z += Math.cos(radians) * speed * dt;
    }

    /** Projects a fixed point on the plane into the ship's {@code (right, forward)} frame. */
    public @NotNull OceanOffset localOf(double virtualX, double virtualZ, double heading) {
        final double radians = Math.toRadians(heading);
        final double deltaX = virtualX - x;
        final double deltaZ = virtualZ - z;

        final double forward = deltaX * -Math.sin(radians) + deltaZ * Math.cos(radians);
        final double right = deltaX * -Math.cos(radians) + deltaZ * -Math.sin(radians);
        return new OceanOffset(right, forward);
    }

    public @NotNull OceanOffset localOf(@NotNull OceanPoint point, double heading) {
        return localOf(point.getX(), point.getZ(), heading);
    }

    /**
     * The point that currently lies at the given bearing and range from the ship.
     *
     * @param bearing degrees off the bow, {@code 0} dead ahead and {@code +90} abeam to starboard
     */
    public @NotNull OceanPoint pointAt(double heading, double bearing, double distance) {
        final double radians = Math.toRadians(heading);
        final double bearingRadians = Math.toRadians(bearing);

        final double forward = distance * Math.cos(bearingRadians);
        final double right = distance * Math.sin(bearingRadians);

        final double pointX = x + forward * -Math.sin(radians) + right * -Math.cos(radians);
        final double pointZ = z + forward * Math.cos(radians) + right * -Math.sin(radians);
        return new OceanPoint(pointX, pointZ);
    }
}
