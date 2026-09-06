package me.mykindos.betterpvp.core.cutscene.camera;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Where the camera is and which way it faces, decoupled from any entity.
 * <p>
 * Kept apart from {@link Location} because a pose is interpolated far more often than it is applied, and a Location
 * carries a world reference that has to agree on both sides of every one of those interpolations.
 */
@Value
public class CameraPose {

    double x;
    double y;
    double z;
    float yaw;
    float pitch;

    public static @NotNull CameraPose of(@NotNull Location location) {
        return new CameraPose(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public @NotNull Location toLocation(@NotNull World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Blends towards {@code target}.
     * <p>
     * Yaw takes the shorter way round rather than the numerically direct one: a pan from 170 to -170 degrees is 20
     * degrees of turn, but interpolating the raw values spins the camera 340 degrees the wrong way. Pitch is left
     * unwrapped because it is clamped to +-90 and cannot wrap.
     *
     * @param progress {@code 0} is this pose, {@code 1} is {@code target}
     */
    public @NotNull CameraPose interpolate(@NotNull CameraPose target, double progress) {
        final float yawDelta = wrapDegrees(target.yaw - yaw);
        return new CameraPose(
                x + (target.x - x) * progress,
                y + (target.y - y) * progress,
                z + (target.z - z) * progress,
                yaw + (float) (yawDelta * progress),
                pitch + (float) ((target.pitch - pitch) * progress));
    }

    /** Folds an angle difference into {@code (-180, 180]} so it describes the shorter rotation. */
    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped > 180f) {
            wrapped -= 360f;
        }
        if (wrapped <= -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }
}
