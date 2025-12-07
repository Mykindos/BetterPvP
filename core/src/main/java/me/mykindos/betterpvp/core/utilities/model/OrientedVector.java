package me.mykindos.betterpvp.core.utilities.model;

import io.papermc.paper.math.FinePosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a vector with an orientation. Similar to a {@link Location}, except no world is attached.
 * @param vector The vector to be oriented.
 * @param pitch  The vertical rotation in degrees.
 * @param yaw    The horizontal rotation in degrees.
 */
@SuppressWarnings("ALL")
public record OrientedVector(@NotNull Vector vector, float pitch, float yaw) implements ConfigurationSerializable, FinePosition {

    public OrientedVector(@NotNull Vector vector, @NotNull Vector orientation) {
        this(vector, computePitch(orientation), computeYaw(orientation));
    }

    public Vector orientation() {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vector(x, y, z);
    }

    private static float computePitch(Vector orientation) {
        double x = orientation.getX();
        double y = orientation.getY();
        double z = orientation.getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        return (float) Math.toDegrees(Math.atan2(-y, horizontal));
    }

    private static float computeYaw(Vector orientation) {
        double x = orientation.getX();
        double z = orientation.getZ();
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
                "vector", vector,
                "pitch", pitch,
                "yaw", yaw
        );
    }

    public static OrientedVector deserialize(@NotNull Map<String, Object> args) {
        Vector vec = (Vector) args.get("vector");
        double pitch = ((double) args.get("pitch"));
        double yaw = ((double) args.get("yaw"));
        return new OrientedVector(vec, (float) pitch, (float) yaw);
    }

    @Override
    public double x() {
        return vector.getX();
    }

    @Override
    public double y() {
        return vector.getY();
    }

    @Override
    public double z() {
        return vector.getZ();
    }

    @Override
    public @NotNull Location toLocation(@NotNull World world) {
        return new Location(world, x(), y(), z(), yaw, pitch);
    }
}
