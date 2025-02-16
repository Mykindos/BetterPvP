package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilVelocity {

    public static Vector getTrajectory2d(Entity from, Entity to) {
        return getTrajectory2d(from.getLocation().toVector(), to.getLocation().toVector());
    }

    public static Vector getTrajectory2d(Location from, Location to) {
        return getTrajectory2d(from.toVector(), to.toVector());
    }

    public static Vector getTrajectory2d(Vector from, Vector to) {
        return to.clone().subtract(from).setY(0).normalize();
    }

    public static void velocity(Entity target, LivingEntity source, VelocityData data) {
        velocity(target, source, data, VelocityType.CUSTOM);
    }

    public static void velocity(Entity target, LivingEntity source, VelocityData data, VelocityType velocityType) {
        Vector vec = data.getVector();
        if (Double.isNaN(vec.getX()) || Double.isNaN(vec.getY()) || Double.isNaN(data.getVector().getZ()) || data.getVector().length() == 0.0D) {
            return;
        }

        if (data.isSetY()) {
            vec.setY(data.getBaseY());
        }

        vec.normalize();
        vec.multiply(data.getStrength());

        vec.setY(vec.getY() + data.getAddY());

        if (vec.getY() > data.getMaxY()) {
            vec.setY(data.getMaxY());
        }

        if (data.isGroundBoost() && UtilBlock.isGrounded(target)) {
            vec.setY(vec.getY() + 0.2D);
        }

        if (data.isResetFallDistance()) {
            target.setFallDistance(0.0F);
        }


        CustomEntityVelocityEvent customEntityVelocityEvent = UtilServer.callEvent(new CustomEntityVelocityEvent(target, source, velocityType, vec));
        if (customEntityVelocityEvent.isCancelled()) return;

        target.setVelocity(customEntityVelocityEvent.getVector());

    }

    /**
     * Apply gravity to a given location and direction.
     * <p>
     * This method will modify the location and direction vectors to simulate gravity.
     *
     * @param location        The current location of the object
     * @param velocity        The current direction of the object
     * @param gravity         The gravity vector, pointing downwards. The default value is (0, -9.81, 0)
     * @param dragCoefficient The drag constant. The default value is 0.2
     * @param elapsedMillis   The time elapsed since the last gravity update
     */
    public static void applyGravity(Location location, Vector velocity, Vector gravity, double dragCoefficient, long elapsedMillis) {
        final double seconds = elapsedMillis / 1000.0;

        // accelerations
        Vector dragAcceleration = velocity.clone().multiply(dragCoefficient);
        Vector totalAcceleration = gravity.clone().subtract(dragAcceleration);

        // calculate new position
        location.add(
                velocity.getX() * seconds + 0.5 * totalAcceleration.getX() * seconds * seconds,
                velocity.getY() * seconds + 0.5 * totalAcceleration.getY() * seconds * seconds,
                velocity.getZ() * seconds + 0.5 * totalAcceleration.getZ() * seconds * seconds
        );

        // calculate new direction
        velocity.add(totalAcceleration.clone().multiply(seconds));
    }

    public static Vector getTrajectory(Entity from, Entity to) {
        return getTrajectory(from.getLocation().toVector(), to.getLocation().toVector());
    }

    public static Vector getTrajectory(Location from, Location to) {
        return getTrajectory(from.toVector(), to.toVector());
    }

    public static Vector getTrajectory(Vector from, Vector to) {
        return to.subtract(from).normalize();
    }
}
