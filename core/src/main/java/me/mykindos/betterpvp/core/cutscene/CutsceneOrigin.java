package me.mykindos.betterpvp.core.cutscene;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Where a viewer stood, and what game mode they were in, at the instant a cutscene took their camera.
 * <p>
 * Spectator is a sticky state: a player whose session dies mid-cutscene - a crash, a restart, a kill - is left
 * invisible and flying with nothing in memory that knows how to put them back. Writing this to disk <em>before</em>
 * the game mode changes is what makes the whole thing recoverable.
 */
@Value
public class CutsceneOrigin {

    @NotNull UUID viewer;
    @NotNull String cutscene;
    @NotNull String world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;
    @NotNull String gameMode;

    public static @NotNull CutsceneOrigin capture(@NotNull UUID viewer, @NotNull String cutscene,
                                                  @NotNull Location location, @NotNull GameMode mode) {
        return new CutsceneOrigin(viewer, cutscene, location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch(), mode.name());
    }

    /** @return the origin location, or {@code null} if its world is no longer loaded */
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
