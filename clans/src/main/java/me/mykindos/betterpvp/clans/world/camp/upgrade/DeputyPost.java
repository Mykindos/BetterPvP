package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/** Where a camp's Deputy Steward stands, in the camp world's coordinates. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeputyPost {

    private int x;
    private int y;
    private int z;
    private float yaw;

    public static @NotNull DeputyPost of(@NotNull Location location) {
        return new DeputyPost(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getYaw());
    }

    /** The middle of the block, facing the way it was placed. */
    public @NotNull Location toLocation(@NotNull World world) {
        final Location at = new Location(world, x + 0.5, y, z + 0.5);
        at.setYaw(yaw);
        return at;
    }
}
