package me.mykindos.betterpvp.core.world.schematic.ghost;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GhostMesherTest {

    private static final BlockData STONE = mock(BlockData.class);
    private static final BlockData PLANKS = mock(BlockData.class);
    private static final BlockData STAIRS = mock(BlockData.class);

    /** Stone and planks are full cubes, stairs are not. */
    private final GhostMesher mesher = new GhostMesher(data -> data != STAIRS);

    @Test
    void aSolidCubeIsDrawnAsOneBoxWithItsMiddleDropped() {
        final List<Schematic.PlacedBlock> cube = box(STONE, 3, 3, 3);

        final List<GhostPiece> pieces = mesher.mesh(cube);

        assertEquals(List.of(new GhostPiece(STONE, 0, 0, 0, 3, 3, 3)), pieces,
                "the middle block is hidden, and the shell around it merges into one box of the same size");
    }

    @Test
    void aRowOfIdenticalBlocksBecomesOneDisplay() {
        final List<GhostPiece> pieces = mesher.mesh(box(STONE, 5, 1, 1));

        assertEquals(List.of(new GhostPiece(STONE, 0, 0, 0, 5, 1, 1)), pieces);
    }

    @Test
    void differentBlocksAreNeverMerged() {
        final List<Schematic.PlacedBlock> row = new ArrayList<>();
        row.add(new Schematic.PlacedBlock(0, 0, 0, STONE));
        row.add(new Schematic.PlacedBlock(1, 0, 0, PLANKS));
        row.add(new Schematic.PlacedBlock(2, 0, 0, STONE));

        assertEquals(3, mesher.mesh(row).size());
    }

    @Test
    void blocksThatAreNotFullCubesStayOnTheirOwn() {
        final List<GhostPiece> pieces = mesher.mesh(box(STAIRS, 4, 1, 1));

        assertEquals(4, pieces.size());
        pieces.forEach(piece -> assertEquals(1, piece.getWidth() * piece.getHeight() * piece.getDepth()));
    }

    @Test
    void aBlockNextToSomethingSeeThroughIsKept() {
        // A stone block buried in stone on five sides and stairs on the sixth can still be seen through the stairs.
        final List<Schematic.PlacedBlock> blocks = new ArrayList<>(box(STONE, 3, 3, 3));
        blocks.removeIf(block -> block.getX() == 1 && block.getY() == 2 && block.getZ() == 1);
        blocks.add(new Schematic.PlacedBlock(1, 2, 1, STAIRS));

        final List<GhostPiece> pieces = mesher.mesh(blocks);
        final int covered = pieces.stream()
                .filter(piece -> piece.getData() == STONE)
                .mapToInt(piece -> piece.getWidth() * piece.getHeight() * piece.getDepth())
                .sum();

        assertEquals(26, covered, "all 26 stone blocks are drawn, the middle one included");
    }

    @Test
    void anOpenShapeUsesFarFewerDisplaysThanBlocks() {
        // A 10 × 5 × 10 hollow room: 4 walls and a floor.
        final List<Schematic.PlacedBlock> room = new ArrayList<>();
        for (Schematic.PlacedBlock block : box(PLANKS, 10, 5, 10)) {
            final boolean wall = block.getX() == 0 || block.getX() == 9 || block.getZ() == 0 || block.getZ() == 9;
            if (wall || block.getY() == 0) {
                room.add(block);
            }
        }

        final List<GhostPiece> pieces = mesher.mesh(room);

        assertEquals(true, pieces.size() <= 10, "was " + pieces.size() + " displays for " + room.size() + " blocks");
    }

    private static List<Schematic.PlacedBlock> box(BlockData data, int width, int height, int depth) {
        final List<Schematic.PlacedBlock> blocks = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    blocks.add(new Schematic.PlacedBlock(x, y, z, data));
                }
            }
        }
        return blocks;
    }
}
