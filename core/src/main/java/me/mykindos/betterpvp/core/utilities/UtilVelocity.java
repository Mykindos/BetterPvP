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

    public static Location getPosition(Location initialPosition, Vector initialVelocity, Vector gravity, double dragConstant, double time) {
        Vector newVelocity = UtilVelocity.getVelocity(initialVelocity, gravity, dragConstant, time);

        Vector dragAcceleration = initialVelocity.clone().multiply(-dragConstant);

        Vector totalAcceleration = gravity.clone().add(dragAcceleration);

        double x = initialPosition.getX() + newVelocity.getX() * time + 0.5 * totalAcceleration.getX() * time * time;
        double y = initialPosition.getY() + newVelocity.getY() * time + 0.5 * totalAcceleration.getY() * time * time;
        double z = initialPosition.getZ() + newVelocity.getZ() * time + 0.5 * totalAcceleration.getZ() * time * time;

        return new Location(initialPosition.getWorld(), x, y, z);
    }

    public static Vector getVelocity(Vector initialVelocity, Vector gravityVector, double dragConstant, double time) {
        Vector dragAcceleration = initialVelocity.clone().multiply(-dragConstant);
        Vector totalAcceleration = gravityVector.clone().add(dragAcceleration);

        return initialVelocity.clone().add(totalAcceleration.multiply(time));
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
