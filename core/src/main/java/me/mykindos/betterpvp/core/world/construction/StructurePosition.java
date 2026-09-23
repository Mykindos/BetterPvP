package me.mykindos.betterpvp.core.world.construction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/** Where a structure's anchor block sits in its world, and how many quarter turns it is turned. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructurePosition {

    private int x;
    private int y;
    private int z;
    private int quarterTurns;

    public static @NotNull StructurePosition of(@NotNull Location anchor, int quarterTurns) {
        return new StructurePosition(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ(),
                Math.floorMod(quarterTurns, 4));
    }

    public @NotNull Location toLocation(@NotNull World world) {
        return new Location(world, x, y, z);
    }
}
