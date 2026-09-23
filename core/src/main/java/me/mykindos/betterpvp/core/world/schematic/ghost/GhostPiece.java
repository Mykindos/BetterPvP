package me.mykindos.betterpvp.core.world.schematic.ghost;

import lombok.Value;
import org.bukkit.block.data.BlockData;

/**
 * One display in a ghost: a block, or a box of identical full blocks drawn as one stretched block. Positions are in
 * blocks from the structure's anchor, already turned.
 */
@Value
public class GhostPiece {
    BlockData data;
    int x;
    int y;
    int z;
    int width;
    int height;
    int depth;
}
