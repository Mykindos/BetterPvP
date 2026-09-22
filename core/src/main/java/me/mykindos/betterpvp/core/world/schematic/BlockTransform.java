package me.mykindos.betterpvp.core.world.schematic;

import lombok.experimental.UtilityClass;
import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * How a structure turns about its anchor, in quarter turns: block positions, continuous points, facings and block data.
 * <p>
 * Blocks and points need two formulas that must stay in register. A block index names the corner of a 1×1 cell rather
 * than its middle, so feeding a datapoint's continuous position through the block formula lands it one block off per
 * quarter turn, a helm marker floating beside the helm instead of on it. {@link #rotatePoint} is derived from the
 * requirement that the <em>centre</em> of block {@code b} lands on the centre of {@link #rotateBlock}{@code (b)}.
 */
@UtilityClass
public class BlockTransform {

    /** Yaw decreases by a quarter turn for each step of {@link #rotateBlock}. */
    private static final float DEGREES_PER_TURN = 90f;

    /**
     * Rotates an anchor-relative block position by {@code quarterTurns} × 90°, the mapping {@code (x, z) -> (z, -x)} per
     * turn.
     *
     * @return a two-element {@code [x, z]} array
     */
    public static int[] rotateBlock(int x, int z, int quarterTurns) {
        int rx = x;
        int rz = z;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            final int nx = rz;
            final int nz = -rx;
            rx = nx;
            rz = nz;
        }
        return new int[]{rx, rz};
    }

    /**
     * Rotates an anchor-relative point by {@code quarterTurns} × 90°, in register with {@link #rotateBlock}.
     *
     * @return a two-element {@code [x, z]} array
     */
    public static double[] rotatePoint(double x, double z, int quarterTurns) {
        double rx = x;
        double rz = z;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            final double nx = rz;
            final double nz = 1 - rx;
            rx = nx;
            rz = nz;
        }
        return new double[]{rx, rz};
    }

    /**
     * Rotates a facing to match {@link #rotateBlock}. Minecraft yaw runs 0 = +Z, 90 = -X, so a quarter turn of the world
     * under a fixed facing is a quarter turn <em>down</em> in yaw.
     *
     * @return the rotated yaw, normalised to {@code [0, 360)}
     */
    public static float rotateYaw(float yaw, int quarterTurns) {
        final float rotated = yaw - DEGREES_PER_TURN * Math.floorMod(quarterTurns, 4);
        return (rotated % 360f + 360f) % 360f;
    }

    /**
     * How many quarter turns take {@code fromYaw} to {@code toYaw}, the rotation to paste with when a structure captured
     * facing one way is placed on a marker facing another.
     * <p>
     * Snapped to the nearest quarter, because block rotation only exists in quarter turns: a marker left at 37° places
     * the structure square rather than refusing or shearing it.
     *
     * @return a value in {@code 0..3}
     */
    public static int quarterTurnsBetween(float fromYaw, float toYaw) {
        final float delta = toYaw - fromYaw;
        // Negated to invert rotateYaw: that subtracts a quarter per turn, so recovering the turn count adds it back.
        return Math.floorMod(-Math.round(delta / DEGREES_PER_TURN), 4);
    }

    /** Turns a block's own facing (logs, stairs, directional and multi-faced blocks) to match {@link #rotateBlock}. */
    public static @NotNull BlockData rotateData(@NotNull BlockData data, int quarterTurns) {
        final int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) {
            return data;
        }
        final BlockData result = data.clone();
        for (int i = 0; i < turns; i++) {
            rotateOnce(result);
        }
        return result;
    }

    private static void rotateOnce(@NotNull BlockData data) {
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
    }

    /** One 90° step matching {@link #rotateBlock}: N→W→S→E and the four diagonals. Vertical faces are unchanged. */
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
}
