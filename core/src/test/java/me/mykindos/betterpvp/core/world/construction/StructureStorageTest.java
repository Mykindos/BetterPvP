package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructureStorageTest {

    private static final BlockData STONE = data(Material.STONE);
    private static final BlockData CHEST = data(Material.CHEST);
    private static final BlockData BARREL = data(Material.BARREL);

    /** Two floors of stone with a chest on the ground floor and a barrel upstairs. */
    private static final Schematic STORE = new Schematic(2, 2, 1, List.of(
            new Schematic.PlacedBlock(0, 0, 0, STONE), new Schematic.PlacedBlock(1, 0, 0, CHEST),
            new Schematic.PlacedBlock(0, 1, 0, BARREL), new Schematic.PlacedBlock(1, 1, 0, STONE)));

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
    void aMoveOrATurnKeepsEachContainersKey() {
        final List<StructureStorage.Slot> here = slots(new Location(null, 0, 64, 0), 0);
        final List<StructureStorage.Slot> there = slots(new Location(null, 40, 70, -12), 1);

        assertEquals(here.stream().map(StructureStorage.Slot::getKey).toList(),
                there.stream().map(StructureStorage.Slot::getKey).toList());
        assertNotEquals(here.get(0).getX(), there.get(0).getX());
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
