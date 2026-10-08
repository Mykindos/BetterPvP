package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructureStorageTest {

    private static final BlockData STONE = data(Material.STONE);
    private static final BlockData CHEST = data(Material.CHEST);
    private static final BlockData BARREL = data(Material.BARREL);

    /** Two floors of stone with a chest on the ground floor and a barrel upstairs. */
    private static final Schematic STORE = new Schematic(2, 2, 1, List.of(
            new Schematic.PlacedBlock(0, 0, 0, STONE), new Schematic.PlacedBlock(1, 0, 0, CHEST),
            new Schematic.PlacedBlock(0, 1, 0, BARREL), new Schematic.PlacedBlock(1, 1, 0, STONE)));

    private static final byte[] BYTES = {1, 2, 3};
    private static final String ENCODED = Base64.getEncoder().encodeToString(BYTES);

    @Test
    void everyContainerIsFoundWithTheLayerItGoesUpWith() {
        final List<StructureStorage.Slot> slots = slots(new Location(null, 0, 64, 0), 0);

        assertEquals(2, slots.size());
        assertEquals("1,0,0", slots.get(0).getKey());
        assertEquals(0, slots.get(0).getLayer());
        assertEquals("0,1,0", slots.get(1).getKey());
        assertEquals(1, slots.get(1).getLayer());
    }

    @Test
    void ac23_aMoveOrATurnKeepsEachContainersKey() {
        final List<StructureStorage.Slot> here = slots(new Location(null, 0, 64, 0), 0);
        final List<StructureStorage.Slot> there = slots(new Location(null, 40, 70, -12), 1);

        assertEquals(here.stream().map(StructureStorage.Slot::getKey).toList(),
                there.stream().map(StructureStorage.Slot::getKey).toList());
        assertNotEquals(here.get(0).getX(), there.get(0).getX());
    }

    @Test
    void ac23_itemsOfAContainerTheBuildLostMoveIntoTheOnesLeft() {
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            final ItemStack stone = item(bukkit);
            final Inventory kept = mock(Inventory.class);
            when(kept.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
            when(kept.getContents()).thenReturn(new ItemStack[]{stone, stone});
            final World world = worldWith(kept);
            final PlacedStructure structure = withStorage("1,0,0", "9,9,9");

            assertTrue(StructureStorage.settle(world, structure, keptSlot(world), new Location(world, 0, 64, 0)));

            verify(kept).addItem(stone);
            verify(world, never()).dropItemNaturally(any(), any(ItemStack.class));
            assertEquals(Set.of("1,0,0"), structure.getStorage().keySet());
        }
    }

    @Test
    void ac23_whateverDoesNotFitDrops() {
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            final ItemStack stone = item(bukkit);
            final Inventory full = mock(Inventory.class);
            when(full.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>(Map.of(0, stone)));
            when(full.getContents()).thenReturn(new ItemStack[]{stone});
            final World world = worldWith(full);
            final Location at = new Location(world, 0, 64, 0);

            StructureStorage.settle(world, withStorage("1,0,0", "9,9,9"), keptSlot(world), at);

            verify(world).dropItemNaturally(at, stone);
        }
    }

    private static List<StructureStorage.Slot> keptSlot(World world) {
        return slots(new Location(world, 0, 64, 0), 0).subList(0, 1);
    }

    /** A structure whose record holds one stone in the container at each of {@code keys}. */
    private static PlacedStructure withStorage(String... keys) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), "store",
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        final Map<String, List<String>> storage = new HashMap<>();
        for (String key : keys) {
            storage.put(key, List.of(ENCODED));
        }
        structure.setStorage(storage);
        return structure;
    }

    /** A world where every block is a chest holding {@code inventory}. */
    private static World worldWith(Inventory inventory) {
        final World world = mock(World.class);
        final Block block = mock(Block.class);
        final Chest chest = mock(Chest.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getState(false)).thenReturn(chest);
        when(chest.getBlockInventory()).thenReturn(inventory);
        return world;
    }

    /** A stone item that encodes as {@link #ENCODED} and decodes back to itself. */
    private static ItemStack item(MockedStatic<Bukkit> bukkit) {
        final ItemStack stone = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.serializeAsBytes()).thenReturn(BYTES);
        final UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.deserializeItem(any())).thenReturn(stone);
        bukkit.when(Bukkit::getUnsafe).thenReturn(unsafe);
        return stone;
    }

    private static List<StructureStorage.Slot> slots(Location anchor, int quarterTurns) {
        return StructureStorage.slots(SchematicPlacement.of(STORE, anchor, quarterTurns), LayerPlan.of(STORE));
    }

    private static BlockData data(Material material) {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        when(data.clone()).thenReturn(data);
        return data;
    }
}
