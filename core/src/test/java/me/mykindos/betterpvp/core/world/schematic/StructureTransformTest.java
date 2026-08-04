package me.mykindos.betterpvp.core.world.schematic;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureTransformTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("no rotation leaves a point untouched")
    void zeroTurnsIsIdentity() {
        final double[] rotated = StructureTransform.rotateXZ(3.5, -7.25, 0);
        assertArrayEquals(new double[]{3.5, -7.25}, rotated, EPSILON);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("four quarter turns return a point to where it started")
    void fourTurnsIsIdentity(int startingTurns) {
        double[] point = {2.5, 9.5};
        for (int turn = 0; turn < 4; turn++) {
            point = StructureTransform.rotateXZ(point[0], point[1], 1);
        }
        assertArrayEquals(new double[]{2.5, 9.5}, point, EPSILON);
    }

    /**
     * The property the whole transform exists for. A datapoint sits at the centre of the block it marks, and the block
     * itself is moved by {@link SchematicAnimator#rotateXZ}; if the two disagree the marker drifts one block per turn
     * and a pasted helm ends up beside the helm.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("a block's centre lands on the centre of where that block is pasted")
    void pointsStayInRegisterWithBlocks(int quarterTurns) {
        for (int blockX = -3; blockX <= 3; blockX++) {
            for (int blockZ = -3; blockZ <= 3; blockZ++) {
                final int[] rotatedBlock = SchematicAnimator.rotateXZ(blockX, blockZ, quarterTurns);
                final double[] rotatedCentre = StructureTransform.rotateXZ(blockX + 0.5, blockZ + 0.5, quarterTurns);

                assertArrayEquals(
                        new double[]{rotatedBlock[0] + 0.5, rotatedBlock[1] + 0.5},
                        rotatedCentre,
                        EPSILON,
                        "block (" + blockX + ", " + blockZ + ") turned " + quarterTurns + " times");
            }
        }
    }

    @Test
    @DisplayName("negative turn counts wrap instead of throwing")
    void negativeTurnsWrap() {
        assertArrayEquals(
                StructureTransform.rotateXZ(4.5, 1.5, 3),
                StructureTransform.rotateXZ(4.5, 1.5, -1),
                EPSILON);
    }

    @Test
    @DisplayName("yaw drops a quarter turn per rotation and wraps into [0, 360)")
    void yawRotates() {
        assertEquals(0f, StructureTransform.rotateYaw(0f, 0), 0.001f);
        assertEquals(270f, StructureTransform.rotateYaw(0f, 1), 0.001f);
        assertEquals(180f, StructureTransform.rotateYaw(0f, 2), 0.001f);
        assertEquals(90f, StructureTransform.rotateYaw(0f, 3), 0.001f);
        assertEquals(0f, StructureTransform.rotateYaw(0f, 4), 0.001f);
    }

    @Test
    @DisplayName("a negative yaw normalises rather than staying negative")
    void yawNormalisesNegativeInput() {
        assertEquals(270f, StructureTransform.rotateYaw(-90f, 0), 0.001f);
        assertEquals(180f, StructureTransform.rotateYaw(-90f, 1), 0.001f);
    }

    /**
     * A facing rotated by N turns must be recoverable as N — this is what lets a berth marker's yaw decide how the hull
     * is placed.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("quarterTurnsBetween inverts rotateYaw")
    void quarterTurnsBetweenInvertsRotateYaw(int turns) {
        final float captureYaw = 45f;
        final float placedYaw = StructureTransform.rotateYaw(captureYaw, turns);
        assertEquals(turns, StructureTransform.quarterTurnsBetween(captureYaw, placedYaw));
    }

    @Test
    @DisplayName("an off-axis marker yaw snaps to the nearest quarter turn rather than being refused")
    void offAxisYawSnaps() {
        // 37 degrees off south is closest to no rotation at all.
        assertEquals(0, StructureTransform.quarterTurnsBetween(0f, 37f));
        // 100 degrees is closest to a single quarter turn.
        assertEquals(3, StructureTransform.quarterTurnsBetween(0f, 100f));
    }

    @Test
    @DisplayName("turns are counted the short way around regardless of how yaw is expressed")
    void equivalentYawsGiveTheSameTurns() {
        assertEquals(
                StructureTransform.quarterTurnsBetween(0f, 90f),
                StructureTransform.quarterTurnsBetween(360f, 450f));
    }

    /**
     * The property a ship's hull is read off: whatever the structure is turned by, the bounds hold every block the
     * paste writes and nothing more. Slack would make somebody standing beside the vessel count as aboard it; a gap
     * would leave part of the deck ashore.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("pasted bounds fit every block a paste writes, exactly")
    void boundsFitThePastedBlocks(int quarterTurns) {
        final int width = 5;
        final int height = 3;
        final int length = 9;
        // An off-centre anchor, as a ship's is: at the stern rather than in the middle of the selection.
        final Schematic schematic = new Schematic(width, height, length, 1, 0, 7, List.of());
        final Location at = new Location(null, 100, 64, -40);

        final BoundingBox bounds = StructureTransform.pastedBounds(schematic, at, quarterTurns);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    final int[] rotated = SchematicAnimator.rotateXZ(x - 1, z - 7, quarterTurns);
                    assertTrue(bounds.contains(
                                    at.getBlockX() + rotated[0] + 0.5,
                                    at.getBlockY() + y + 0.5,
                                    at.getBlockZ() + rotated[1] + 0.5),
                            "block (" + x + ", " + y + ", " + z + ") turned " + quarterTurns + " times");
                }
            }
        }

        assertEquals(width * height * length, bounds.getVolume(), EPSILON,
                "the box is the selection itself, with no slack around it");
    }
}
