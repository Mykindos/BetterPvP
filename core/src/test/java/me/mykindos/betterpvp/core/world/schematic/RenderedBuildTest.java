package me.mykindos.betterpvp.core.world.schematic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Raises and lowers a three-level tower in a fake world that only remembers which blocks were written where. */
class RenderedBuildTest {

    private static final BlockData GRASS = data(Material.SHORT_GRASS);
    private static final BlockData STONE = data(Material.STONE);

    private final Map<String, BlockData> blocks = new HashMap<>();
    private final UUID id = UUID.randomUUID();
    private BlockBatchStore store;
    private SchematicRenderer renderer;
    private SchematicPlacement placement;
    private LayerPlan plan;

    @BeforeEach
    void setUp() {
        final World world = mock(World.class);
        when(world.getName()).thenReturn("camp");
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        // Grass at the tower's base, which the first layer must hand back untouched.
        blocks.put(key(0, 64, 0), GRASS);

        store = mock(BlockBatchStore.class);
        when(store.get(any(), anyString())).thenReturn(Optional.empty());
        renderer = new SchematicRenderer(store);

        final Schematic tower = new Schematic(1, 3, 1, List.of(
                new Schematic.PlacedBlock(0, 0, 0, STONE),
                new Schematic.PlacedBlock(0, 1, 0, STONE),
                new Schematic.PlacedBlock(0, 2, 0, STONE)));
        placement = SchematicPlacement.of(tower, new Location(world, 0, 64, 0), 0);
        plan = LayerPlan.of(tower);
    }

    @Test
    void raisingShowsLayersFromTheBottom() {
        final RenderedBuild build = renderer.open(placement, plan, id);

        build.showUpTo(2);

        assertEquals(2, build.getShown());
        assertEquals(STONE, blocks.get(key(0, 64, 0)));
        assertEquals(STONE, blocks.get(key(0, 65, 0)));
        assertFalse(blocks.containsKey(key(0, 66, 0)));
        verify(store).save(eq(id), eq("camp"), anyList());
    }

    @Test
    void loweringTakesTheTopLayersBackOut() {
        final RenderedBuild build = renderer.open(placement, plan, id);
        build.showAll();

        build.showUpTo(1);

        assertEquals(STONE, blocks.get(key(0, 64, 0)));
        assertEquals(Material.AIR, blocks.get(key(0, 65, 0)).getMaterial());
        assertEquals(Material.AIR, blocks.get(key(0, 66, 0)).getMaterial());
    }

    @Test
    void revertingHandsTheGroundBackAndForgetsTheRecord() {
        final RenderedBuild build = renderer.open(placement, plan, id);
        build.showAll();
        assertTrue(build.isComplete());

        build.revert();

        assertEquals(0, build.getShown());
        assertEquals(GRASS, blocks.get(key(0, 64, 0)));
        verify(store, atLeastOnce()).clear(id, "camp");
    }

    @Test
    void showingPastThePlanStopsAtTheTop() {
        final RenderedBuild build = renderer.open(placement, plan, id);

        build.showUpTo(99);

        assertEquals(3, build.getShown());
    }

    @Test
    void openingTakesOutWhatAPreviousRunLeftStanding() {
        blocks.put(key(0, 65, 0), STONE);
        final BlockBatchStore.Batch leftover = new BlockBatchStore.Batch(id, "camp",
                List.of(new Schematic.PlacedBlock(0, 65, 0, GRASS)));
        when(store.get(id, "camp")).thenReturn(Optional.of(leftover));

        renderer.open(placement, plan, id);

        assertEquals(GRASS, blocks.get(key(0, 65, 0)));
        verify(store, never()).save(any(), anyString(), anyList());
    }

    private Block block(int x, int y, int z) {
        final Block block = mock(Block.class);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        when(block.getBlockData()).thenAnswer(invocation -> blocks.getOrDefault(key(x, y, z), data(Material.AIR)));
        doAnswer(invocation -> {
            blocks.put(key(x, y, z), invocation.getArgument(0));
            return null;
        }).when(block).setBlockData(any(BlockData.class), anyBoolean());
        return block;
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static BlockData data(Material material) {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        when(data.clone()).thenReturn(data);
        return data;
    }
}
