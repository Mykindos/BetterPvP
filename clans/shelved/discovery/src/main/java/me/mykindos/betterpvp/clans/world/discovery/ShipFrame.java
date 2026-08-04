package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The real hull's own axes: where it sits in the world and which way it points.
 * <p>
 * There are two rotations in play out at sea and conflating them is the easiest way to get the whole feature wrong.
 * The first is the <em>virtual</em> heading, which lives in {@link ShipDynamics} and describes where the ship is
 * pointing on the imaginary plane it sails across; {@link Ocean#localOf} has already divided that out by the time
 * anything reaches here, which is why an {@link OceanOffset} is expressed as "so far ahead, so far to starboard"
 * rather than as coordinates. The second is the <em>real</em> orientation of the hull that is standing still in the
 * limbo world, taken from the yaw of the berth marker its structure was pasted against. That is this class.
 * <p>
 * So the job is only ever to lay a ship-local offset back down onto a hull that never moves. The unit vectors follow
 * the same Minecraft-yaw convention {@link ShipDynamics} documents:
 * <pre>
 * forward = (-sin(yaw),  cos(yaw))
 * right   = (-cos(yaw), -sin(yaw))
 * </pre>
 * Pure numbers throughout, so the geometry the rest of the feature rests on is checkable without a world.
 */
@Value
public class ShipFrame {

    /** Where the bow-forward, starboard-right axes are rooted — the berth's paste anchor. */
    double originX;

    double originZ;

    /** The anchor's facing in degrees, the direction the moored hull actually points. */
    double yaw;

    public static @NotNull ShipFrame of(double originX, double originZ, double yaw) {
        return new ShipFrame(originX, originZ, yaw);
    }

    public double forwardX() {
        return -Math.sin(Math.toRadians(yaw));
    }

    public double forwardZ() {
        return Math.cos(Math.toRadians(yaw));
    }

    public double rightX() {
        return -Math.cos(Math.toRadians(yaw));
    }

    public double rightZ() {
        return -Math.sin(Math.toRadians(yaw));
    }

    /** Where an offset off the bow and beam falls in world {@code x}. */
    public double worldX(@NotNull OceanOffset offset) {
        return originX + forwardX() * offset.getForward() + rightX() * offset.getRight();
    }

    /** Where an offset off the bow and beam falls in world {@code z}. */
    public double worldZ(@NotNull OceanOffset offset) {
        return originZ + forwardZ() * offset.getForward() + rightZ() * offset.getRight();
    }
}
