package me.mykindos.betterpvp.core.utilities.model.selector.origin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the origin point for a selector operation.
 * The origin provides position, world, and optional orientation information.
 */
public interface SelectorOrigin {

    /**
     * Gets the world this origin is in.
     *
     * @return the world
     */
    World getWorld();

    /**
     * Gets the position of this origin as a vector.
     *
     * @return the position vector
     */
    Vector getPosition();

    /**
     * Converts this origin to a Bukkit Location.
     *
     * @return the location representing this origin
     */
    Location toLocation();

    /**
     * Gets the orientation (direction) of this origin, if available.
     * This is used for directional shapes like arcs and oriented boxes.
     *
     * @return the direction vector, or null if no orientation is available
     */
    @Nullable
    Vector getOrientation();

    /**
     * Gets the pitch angle of this origin in degrees.
     * Pitch is the vertical angle, where -90 is straight up and 90 is straight down.
     *
     * @return the pitch angle, or 0 if no orientation is available
     */
    default float getPitch() {
        Vector orientation = getOrientation();
        if (orientation == null) {
            return 0f;
        }
        // Calculate pitch from direction vector
        // Pitch is the angle from the horizontal plane
        double xz = Math.sqrt(orientation.getX() * orientation.getX() + orientation.getZ() * orientation.getZ());
        return (float) Math.toDegrees(-Math.atan2(orientation.getY(), xz));
    }

    /**
     * Gets the yaw angle of this origin in degrees.
     * Yaw is the horizontal rotation, where 0 is south, 90 is west, 180 is north, 270 is east.
     *
     * @return the yaw angle, or 0 if no orientation is available
     */
    default float getYaw() {
        Vector orientation = getOrientation();
        if (orientation == null) {
            return 0f;
        }
        // Calculate yaw from direction vector
        // Yaw is the angle in the XZ plane from the positive Z axis
        return (float) Math.toDegrees(Math.atan2(-orientation.getX(), orientation.getZ()));
    }
}
