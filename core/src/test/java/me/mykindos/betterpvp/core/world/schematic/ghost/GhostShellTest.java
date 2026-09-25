package me.mykindos.betterpvp.core.world.schematic.ghost;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GhostShellTest {

    private static final BlockData STONE = mock(BlockData.class);
    private static final BlockData STAIRS = mock(BlockData.class);

    /** Stone is a full cube, stairs are not. */
    private final GhostShell shell = new GhostShell(data -> data != STAIRS);

    @Test
    void aSolidCubeDropsOnlyItsMiddle() {
        final List<Schematic.PlacedBlock> visible = shell.visible(box(STONE, 3, 3, 3));

        assertEquals(26, visible.size());
        assertFalse(visible.contains(new Schematic.PlacedBlock(1, 1, 1, STONE)));
    }

    @Test
    void aWallIsDrawnOneBlockAtATime() {
        final List<Schematic.PlacedBlock> wall = box(STONE, 5, 5, 1);

        assertEquals(wall, shell.visible(wall));
    }

    @Test
    void eachBlockKeepsItsOwnState() {
        final BlockData closedDoor = mock(BlockData.class);
        final BlockData openDoor = mock(BlockData.class);
        final List<Schematic.PlacedBlock> doors = List.of(
                new Schematic.PlacedBlock(0, 0, 0, closedDoor),
                new Schematic.PlacedBlock(1, 0, 0, openDoor));

        assertEquals(doors, shell.visible(doors));
    }

    @Test
    void aBlockNextToSomethingSeeThroughIsKept() {
        // The middle stone block is buried in stone on five sides and stairs on the sixth, so it shows through the stairs.
        final List<Schematic.PlacedBlock> blocks = new ArrayList<>(box(STONE, 3, 3, 3));
        blocks.removeIf(block -> block.getX() == 1 && block.getY() == 2 && block.getZ() == 1);
        blocks.add(new Schematic.PlacedBlock(1, 2, 1, STAIRS));

        final List<Schematic.PlacedBlock> visible = shell.visible(blocks);

        assertEquals(27, visible.size());
        assertTrue(visible.contains(new Schematic.PlacedBlock(1, 1, 1, STONE)));
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
