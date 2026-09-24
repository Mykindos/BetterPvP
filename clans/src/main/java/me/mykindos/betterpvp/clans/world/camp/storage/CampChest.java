package me.mykindos.betterpvp.clans.world.camp.storage;

import lombok.Value;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStorage;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** One marked chest in a structure's build as it stands, and the storage slot its contents are kept under. */
@Value
public class CampChest {

    PlacedStructure structure;
    /** Its place among the chests of the same mark in its structure, 1 being the first. */
    int index;
    int x;
    int y;
    int z;
    /** Null when the marked block is not a container. */
    @Nullable StructureStorage.Slot slot;

    public @NotNull Block block(@NotNull World world) {
        return world.getBlockAt(x, y, z);
    }

    public boolean isAt(@NotNull Block block) {
        return block.getX() == x && block.getY() == y && block.getZ() == z;
    }
}
