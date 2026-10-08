package me.mykindos.betterpvp.core.world.settler.presence;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Stubs a mocked world's blocks as solid at one height and air everywhere else, or air everywhere. */
final class FakeGround {

    private FakeGround() {
    }

    /** Solid ground at {@code floorY}, air above and below it. */
    static void floorAt(World world, int floorY) {
        stub(world, floorY);
    }

    /** No ground anywhere. */
    static void none(World world) {
        stub(world, Integer.MIN_VALUE);
    }

    private static void stub(World world, int floorY) {
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(world, floorY, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        when(world.getBlockAt(any(Location.class))).thenAnswer(invocation -> {
            final Location at = invocation.getArgument(0);
            return block(world, floorY, at.getBlockX(), at.getBlockY(), at.getBlockZ());
        });
    }

    private static Block block(World world, int floorY, int x, int y, int z) {
        final boolean solid = y == floorY;
        final Block block = mock(Block.class);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(solid ? Material.STONE : Material.AIR);
        when(block.isSolid()).thenReturn(solid);
        when(block.isPassable()).thenReturn(!solid);
        when(block.isEmpty()).thenReturn(!solid);
        when(block.isLiquid()).thenReturn(false);
        when(block.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> block(world, floorY,
                x + invocation.<Integer>getArgument(0), y + invocation.<Integer>getArgument(1),
                z + invocation.<Integer>getArgument(2)));
        when(block.getRelative(any(BlockFace.class))).thenAnswer(invocation -> {
            final BlockFace face = invocation.getArgument(0);
            return block(world, floorY, x + face.getModX(), y + face.getModY(), z + face.getModZ());
        });
        return block;
    }
}
