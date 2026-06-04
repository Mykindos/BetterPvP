package me.mykindos.betterpvp.core.world.schematic;

import com.google.inject.Singleton;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pastes {@link Schematic}s into the world around their {@link Schematic#getAnchorX() anchor} (the {@code //copy}
 * origin), optionally rotated about the vertical axis in 90° steps. The schematic's anchor block lands on the paste
 * {@link Location}, so the author controls exactly which block sits there. Rotation transforms both block positions and
 * each block's facing (logs, stairs, directional blocks). {@link #pasteCapturing} / {@link #restore} are the
 * non-destructive pair ({@code //paste -a} then {@code //undo}) used to overlay and back out animation frames without
 * disturbing surrounding blocks.
 */
@Singleton
public class SchematicAnimator {

    /**
     * Instantly applies every block of {@code schematic}, rotated {@code quarterTurns} × 90° about its anchor, with the
     * anchor block placed at {@code at}. Physics is suppressed so neighbouring blocks do not react mid-paste.
     */
    public void paste(@NotNull Schematic schematic, @NotNull Location at, int quarterTurns) {
        final World world = at.getWorld();
        final int ax = at.getBlockX();
        final int ay = at.getBlockY();
        final int az = at.getBlockZ();
        final int turns = quarterTurns & 3;
        for (Schematic.PlacedBlock block : schematic.getBlocks()) {
            final int lx = block.getX() - schematic.getAnchorX();
            final int ly = block.getY() - schematic.getAnchorY();
            final int lz = block.getZ() - schematic.getAnchorZ();
            final int[] rotated = rotateXZ(lx, lz, turns);
            world.getBlockAt(ax + rotated[0], ay + ly, az + rotated[1])
                    .setBlockData(rotateData(block.getData(), turns), false);
        }
    }

    /**
     * Overlays only the <em>solid</em> blocks of {@code schematic} (the {@code //paste -a} of FAWE — air voxels are
     * skipped, so surrounding decoration is untouched), rotated and anchored exactly like {@link #paste}, and returns
     * the blocks it overwrote so the paste can be backed out later. Each returned {@link Schematic.PlacedBlock} holds
     * an <em>absolute</em> world position and the data that was there <em>before</em> this paste; feeding the list to
     * {@link #restore} is the {@code //undo}. Physics is suppressed, matching {@link #paste}.
     *
     * @return the captured pre-paste blocks, to hand to {@link #restore} when exiting this frame
     */
    public @NotNull List<Schematic.PlacedBlock> pasteCapturing(@NotNull World world, @NotNull Schematic schematic,
                                                               @NotNull Location at, int quarterTurns) {
        final int ax = at.getBlockX();
        final int ay = at.getBlockY();
        final int az = at.getBlockZ();
        final int turns = quarterTurns & 3;
        final List<Schematic.PlacedBlock> captured = new ArrayList<>();
        for (Schematic.PlacedBlock block : schematic.getBlocks()) {
            if (block.getData().getMaterial().isAir()) {
                continue; // -a : never write (or capture) air, so the cell's existing content is left alone
            }
            final int[] rotated = rotateXZ(block.getX() - schematic.getAnchorX(), block.getZ() - schematic.getAnchorZ(), turns);
            final Block worldBlock = world.getBlockAt(ax + rotated[0], ay + block.getY() - schematic.getAnchorY(), az + rotated[1]);
            captured.add(new Schematic.PlacedBlock(worldBlock.getX(), worldBlock.getY(), worldBlock.getZ(), worldBlock.getBlockData()));
            worldBlock.setBlockData(rotateData(block.getData(), turns), false);
        }
        return captured;
    }

    /**
     * Restores blocks captured by {@link #pasteCapturing} (the {@code //undo}): each entry's stored data is written
     * back at its absolute position, returning those cells to exactly what they held before the matching paste.
     * Physics is suppressed, matching {@link #paste}.
     */
    public void restore(@NotNull World world, @NotNull List<Schematic.PlacedBlock> captured) {
        for (Schematic.PlacedBlock block : captured) {
            world.getBlockAt(block.getX(), block.getY(), block.getZ()).setBlockData(block.getData(), false);
        }
    }

    /**
     * Rotates an anchor-relative (x, z) by {@code quarterTurns} × 90° (the mapping {@code (x,z) -> (z,-x)} per turn).
     * Shared by paste and bounds computation so callers stay consistent.
     *
     * @return a two-element {@code [x, z]} array
     */
    public static int[] rotateXZ(int x, int z, int quarterTurns) {
        int rx = x;
        int rz = z;
        for (int i = 0; i < (quarterTurns & 3); i++) {
            final int nx = rz;
            final int nz = -rx;
            rx = nx;
            rz = nz;
        }
        return new int[]{rx, rz};
    }

    private static @NotNull BlockData rotateData(@NotNull BlockData data, int quarterTurns) {
        final int turns = quarterTurns & 3;
        if (turns == 0) {
            return data;
        }
        BlockData result = data.clone();
        for (int i = 0; i < turns; i++) {
            result = rotateOnce(result);
        }
        return result;
    }

    private static @NotNull BlockData rotateOnce(@NotNull BlockData data) {
        if (data instanceof Orientable orientable) {
            final Axis axis = orientable.getAxis();
            if (axis == Axis.X) {
                orientable.setAxis(Axis.Z);
            } else if (axis == Axis.Z) {
                orientable.setAxis(Axis.X);
            }
        } else if (data instanceof Rotatable rotatable) {
            rotatable.setRotation(rotateFace(rotatable.getRotation()));
        } else if (data instanceof Directional directional) {
            final BlockFace next = rotateFace(directional.getFacing());
            if (directional.getFaces().contains(next)) {
                directional.setFacing(next);
            }
        } else if (data instanceof MultipleFacing multipleFacing) {
            final Set<BlockFace> allowed = multipleFacing.getAllowedFaces();
            final Map<BlockFace, Boolean> rotated = new EnumMap<>(BlockFace.class);
            for (BlockFace face : allowed) {
                rotated.put(rotateFace(face), multipleFacing.hasFace(face));
            }
            for (BlockFace face : allowed) {
                final Boolean value = rotated.get(face);
                multipleFacing.setFace(face, value != null && value);
            }
        }
        return data;
    }

    /** One 90° step matching {@link #rotateXZ}: N→W→S→E and the four diagonals; vertical/unknown faces unchanged. */
    private static @NotNull BlockFace rotateFace(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case NORTH_WEST -> BlockFace.SOUTH_WEST;
            case SOUTH_WEST -> BlockFace.SOUTH_EAST;
            case SOUTH_EAST -> BlockFace.NORTH_EAST;
            case NORTH_EAST -> BlockFace.NORTH_WEST;
            default -> face;
        };
    }

    /**
     * Computes the world bounding box of {@code schematic} pasted at {@code at} with {@code quarterTurns} rotation, as a
     * {@code [minX, minY, minZ, maxX, maxY, maxZ]} array of block coordinates. Empty schematics yield a 1×1×1 box at
     * {@code at}.
     */
    public static int[] bounds(@NotNull Schematic schematic, @NotNull Location at, int quarterTurns) {
        final int ax = at.getBlockX();
        final int ay = at.getBlockY();
        final int az = at.getBlockZ();
        final int turns = quarterTurns & 3;
        final List<Schematic.PlacedBlock> blocks = schematic.getBlocks();
        if (blocks.isEmpty()) {
            return new int[] {
                    ax, ay, az,
                    ax, ay, az
            };
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Schematic.PlacedBlock block : blocks) {
            final int[] rotated = rotateXZ(block.getX() - schematic.getAnchorX(), block.getZ() - schematic.getAnchorZ(), turns);
            final int wx = ax + rotated[0];
            final int wy = ay + block.getY() - schematic.getAnchorY();
            final int wz = az + rotated[1];
            minX = Math.min(minX, wx);
            minY = Math.min(minY, wy);
            minZ = Math.min(minZ, wz);
            maxX = Math.max(maxX, wx);
            maxY = Math.max(maxY, wy);
            maxZ = Math.max(maxZ, wz);
        }
        return new int[] {
                minX, minY, minZ,
                maxX, maxY, maxZ
        };
    }
}
