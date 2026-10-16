package me.mykindos.betterpvp.core.world.schematic;

import me.mykindos.betterpvp.structure.BlockTransform;
import me.mykindos.betterpvp.structure.Schematic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchematicPlacementTest {

    private static final BlockData STONE = data(Material.STONE);
    private static final BlockData AIR = data(Material.AIR);

    /** An L: three blocks along x, and one more going off in z from the first. */
    private static final Schematic L_SHAPE = new Schematic(3, 1, 2, List.of(
            block(0, 0, 0), block(1, 0, 0), block(2, 0, 0), block(0, 0, 1),
            new Schematic.PlacedBlock(1, 0, 1, AIR), new Schematic.PlacedBlock(2, 0, 1, AIR)));

    @Test
    void airIsNeverPlaced() {
        final SchematicPlacement placement = SchematicPlacement.of(L_SHAPE, at(0, 64, 0), 0);

        assertEquals(4, placement.getBlocks().size());
        assertEquals(4, placement.getFootprint().getColumns().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void theBlockBoundsFitTheSolidBlocksExactly(int quarterTurns) {
        final SchematicPlacement placement = SchematicPlacement.of(L_SHAPE, at(10, 64, -5), quarterTurns);
        final BoundingBox bounds = placement.blockBounds();

        for (Schematic.PlacedBlock block : placement.getBlocks()) {
            assertTrue(bounds.contains(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5));
        }
        assertEquals(6, bounds.getVolume(), 0.0001, "3 by 2 by 1, the L's own box");
    }

    @Test
    void twoLShapesCanNestIntoEachOthersCorners() {
        final SchematicPlacement first = SchematicPlacement.of(L_SHAPE, at(0, 64, 0), 0);
        // Turned half way round and tucked into the first L's open corner: the boxes overlap, the blocks do not.
        final SchematicPlacement second = SchematicPlacement.of(L_SHAPE, at(3, 64, 1), 2);

        assertTrue(first.blockBounds().overlaps(second.blockBounds()));
        assertFalse(first.getFootprint().intersects(second.getFootprint()));
    }

    @Test
    void sharingAColumnAtTheSameHeightIntersects() {
        final SchematicPlacement first = SchematicPlacement.of(L_SHAPE, at(0, 64, 0), 0);
        final SchematicPlacement overlapping = SchematicPlacement.of(L_SHAPE, at(2, 64, 0), 0);

        assertTrue(first.getFootprint().intersects(overlapping.getFootprint()));
    }

    @Test
    void sharingAColumnAtDifferentHeightsDoesNotIntersect() {
        final SchematicPlacement first = SchematicPlacement.of(L_SHAPE, at(0, 64, 0), 0);
        final SchematicPlacement above = SchematicPlacement.of(L_SHAPE, at(0, 70, 0), 0);

        assertFalse(first.getFootprint().intersects(above.getFootprint()));
    }

    @Test
    void placingMovesEveryBlockByTheAnchor() {
        final SchematicPlacement placement = SchematicPlacement.of(L_SHAPE, at(100, 64, 200), 0);
        final List<int[]> positions = new ArrayList<>();
        placement.getBlocks().forEach(block -> positions.add(new int[]{block.getX(), block.getY(), block.getZ()}));

        assertEquals(100, positions.getFirst()[0]);
        assertEquals(64, positions.getFirst()[1]);
        assertEquals(200, positions.getFirst()[2]);
    }

    private static Location at(int x, int y, int z) {
        return new Location(null, x, y, z);
    }

    private static Schematic.PlacedBlock block(int x, int y, int z) {
        return new Schematic.PlacedBlock(x, y, z, STONE);
    }

    private static BlockData data(Material material) {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        when(data.clone()).thenReturn(data);
        return data;
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

        final BoundingBox bounds = SchematicPlacement.of(schematic, at, quarterTurns).selectionBounds();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    final int[] rotated = BlockTransform.rotateBlock(x - 1, z - 7, quarterTurns);
                    assertTrue(bounds.contains(
                                    at.getBlockX() + rotated[0] + 0.5,
                                    at.getBlockY() + y + 0.5,
                                    at.getBlockZ() + rotated[1] + 0.5),
                            "block (" + x + ", " + y + ", " + z + ") turned " + quarterTurns + " times");
                }
            }
        }

        assertEquals(width * height * length, bounds.getVolume(), 1e-9,
                "the box is the selection itself, with no slack around it");
    }
}
