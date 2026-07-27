package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Where a rider stood, and what game mode they were in, at the instant they clicked the cannon.
 * <p>
 * Written to disk before anything else happens: a rider who disconnects mid-flight would otherwise be stranded in
 * spectator wherever the mannequin was, which no in-memory session can repair after a crash or restart. Holding the
 * origin durably keeps the ride abortable at any moment.
 */
@Value
public class RideOrigin {

    @NotNull UUID rider;
    @NotNull String world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;
    @NotNull String gameMode;

    public static @NotNull RideOrigin capture(@NotNull UUID rider, @NotNull Location location, @NotNull GameMode mode) {
        return new RideOrigin(rider, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), mode.name());
    }

    /** @return the origin location, or {@code null} if its world is not loaded */
    public @Nullable Location toLocation() {
        final World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public @NotNull GameMode toGameMode() {
        try {
            return GameMode.valueOf(gameMode);
        } catch (IllegalArgumentException exception) {
            return GameMode.SURVIVAL;
        }
    }
}
