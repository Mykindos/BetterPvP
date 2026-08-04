package me.mykindos.betterpvp.core.world.schematic;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/**
 * The shared rotation maths for pasting a structure: how a continuous point and a facing move when the structure is
 * turned about its anchor.
 * <p>
 * Blocks are rotated by {@link SchematicAnimator#rotateXZ(int, int, int)}, which works on <b>block indices</b>. A block
 * index names the corner of a 1×1 cell rather than its middle, so feeding a datapoint's continuous position through that
 * same formula lands it one block off per quarter turn — a helm marker floating beside the helm instead of on it. The
 * point mapping here carries the correction that keeps the two in register: it is derived from the requirement that the
 * <em>centre</em> of block {@code b} must land on the centre of {@code rotateXZ(b)}.
 */
@UtilityClass
public class StructureTransform {

    /** Yaw decreases by a quarter turn for each step of {@link SchematicAnimator#rotateXZ}. */
    private static final float DEGREES_PER_TURN = 90f;

    /**
     * Rotates an anchor-relative point by {@code quarterTurns} × 90°, in register with the block rotation.
     *
     * @return a two-element {@code [x, z]} array
     */
    public static double[] rotateXZ(double x, double z, int quarterTurns) {
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
     * Rotates a facing to match {@link #rotateXZ}. Minecraft yaw runs 0 = +Z, 90 = -X, so a quarter turn of the world
     * under a fixed facing is a quarter turn <em>down</em> in yaw.
     *
     * @return the rotated yaw, normalised to {@code [0, 360)}
     */
    public static float rotateYaw(float yaw, int quarterTurns) {
        final float rotated = yaw - DEGREES_PER_TURN * Math.floorMod(quarterTurns, 4);
        return (rotated % 360f + 360f) % 360f;
    }

    /**
     * How many quarter turns take {@code fromYaw} to {@code toYaw} — the rotation to paste with when a structure
     * captured facing one way is placed on a marker facing another.
     * <p>
     * Snapped to the nearest quarter, because block rotation only exists in quarter turns: a berth marker left at 37°
     * places the hull square rather than refusing or shearing it.
     *
     * @return a value in {@code 0..3}
     */
    public static int quarterTurnsBetween(float fromYaw, float toYaw) {
        final float delta = toYaw - fromYaw;
        // Negated to invert rotateYaw: that subtracts a quarter per turn, so recovering the turn count adds it back.
        return Math.floorMod(-Math.round(delta / DEGREES_PER_TURN), 4);
    }

    /**
     * The world-space volume a paste of {@code schematic} occupies — the captured selection itself, turned and moved to
     * where it lands.
     * <p>
     * This is what a structure is <em>for</em> asking about: the region a builder drew around the build is already the
     * build's extent, so anything that needs to know whether somebody is inside one can read it off the capture rather
     * than having a volume authored a second time as a data-point and kept in step by hand.
     * <p>
     * The box spans whole blocks: a block at the far corner is inside it, not on its face.
     *
     * @param at           where the anchor block lands
     * @param quarterTurns how far the structure is turned about that anchor
     */
    public static @NotNull BoundingBox pastedBounds(@NotNull Schematic schematic, @NotNull Location at,
                                                    int quarterTurns) {
        final int minX = -schematic.getAnchorX();
        final int minZ = -schematic.getAnchorZ();
        final int maxX = schematic.getWidth() - 1 - schematic.getAnchorX();
        final int maxZ = schematic.getLength() - 1 - schematic.getAnchorZ();

        // All four corners, because a quarter turn swaps which pair of them is opposite: rotating only min and max
        // gives the right box for a half turn and a sheared one for a quarter.
        final int[][] corners = {
                SchematicAnimator.rotateXZ(minX, minZ, quarterTurns),
                SchematicAnimator.rotateXZ(minX, maxZ, quarterTurns),
                SchematicAnimator.rotateXZ(maxX, minZ, quarterTurns),
                SchematicAnimator.rotateXZ(maxX, maxZ, quarterTurns)};

        int lowX = corners[0][0];
        int highX = corners[0][0];
        int lowZ = corners[0][1];
        int highZ = corners[0][1];
        for (int[] corner : corners) {
            lowX = Math.min(lowX, corner[0]);
            highX = Math.max(highX, corner[0]);
            lowZ = Math.min(lowZ, corner[1]);
            highZ = Math.max(highZ, corner[1]);
        }

        final int baseY = at.getBlockY() - schematic.getAnchorY();
        return new BoundingBox(
                at.getBlockX() + lowX, baseY, at.getBlockZ() + lowZ,
                at.getBlockX() + highX + 1, baseY + schematic.getHeight(), at.getBlockZ() + highZ + 1);
    }
}
