package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.Region;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LayerPlanTest {

    private static final BlockData STONE = data(Material.STONE);
    private static final BlockData AIR = data(Material.AIR);

    @Test
    void everyLevelIsALayerFromTheBottomUpAndAirIsLeftOut() {
        final Schematic schematic = new Schematic(1, 3, 2, List.of(
                block(0, 2, 0), block(0, 0, 0), block(0, 1, 0), block(0, 1, 1),
                new Schematic.PlacedBlock(0, 2, 1, AIR)));

        final LayerPlan plan = LayerPlan.of(schematic);

        assertEquals(3, plan.size());
        assertEquals(List.of(0), heights(plan.layer(0)));
        assertEquals(List.of(1, 1), heights(plan.layer(1)));
        assertEquals(List.of(2), heights(plan.layer(2)));
    }

    @Test
    void aMarkedCuboidGoesDownAsItsOwnLayerInKeyOrder() {
        // The anchor sits at (1, 0, 0), so the marker's anchor-relative corner (-1, 2, 0) is the block at (0, 2, 0).
        final CapturedRegion scaffolding = CapturedRegion.builder()
                .name("scaffold")
                .type(Region.RegionType.CUBOID)
                .tags(Set.of("layer:-1"))
                .points(List.of(point(-1, 2, 0), point(-1, 2, 0)))
                .build();
        final Schematic schematic = new Schematic(2, 3, 1, 1, 0, 0,
                List.of(block(0, 0, 0), block(0, 1, 0), block(0, 2, 0), block(1, 2, 0)), List.of(scaffolding), 0f);

        final LayerPlan plan = LayerPlan.of(schematic);

        assertEquals(4, plan.size());
        assertEquals(List.of(new Schematic.PlacedBlock(0, 2, 0, STONE)), plan.layer(0));
        assertEquals(List.of(0), heights(plan.layer(1)));
        assertEquals(List.of(new Schematic.PlacedBlock(1, 2, 0, STONE)), plan.layer(3));
    }

    @Test
    void bothHalvesOfADoorGoDownTogether() {
        final Schematic.PlacedBlock lower = new Schematic.PlacedBlock(1, 1, 0, door(Bisected.Half.BOTTOM));
        final Schematic.PlacedBlock upper = new Schematic.PlacedBlock(1, 2, 0, door(Bisected.Half.TOP));
        final LayerPlan plan = LayerPlan.of(new Schematic(2, 3, 1,
                List.of(block(0, 0, 0), block(0, 1, 0), block(0, 2, 0), lower, upper)));

        assertEquals(3, plan.size());
        assertEquals(List.of(block(0, 1, 0), lower, upper), plan.layer(1));
        assertEquals(List.of(block(0, 2, 0)), plan.layer(2));
    }

    @Test
    void progressMapsToWholeLayers() {
        assertEquals(0, LayerPlan.layersAt(0.0, 10));
        assertEquals(4, LayerPlan.layersAt(0.49, 10));
        assertEquals(10, LayerPlan.layersAt(1.0, 10));
        assertEquals(10, LayerPlan.layersAt(3.0, 10));
        assertEquals(0, LayerPlan.layersAt(-1.0, 10));
    }

    private static List<Integer> heights(List<Schematic.PlacedBlock> layer) {
        return layer.stream().map(Schematic.PlacedBlock::getY).toList();
    }

    private static Schematic.PlacedBlock block(int x, int y, int z) {
        return new Schematic.PlacedBlock(x, y, z, STONE);
    }

    private static CapturedRegion.RelativePoint point(double x, double y, double z) {
        return CapturedRegion.RelativePoint.builder().x(x).y(y).z(z).build();
    }

    private static Door door(Bisected.Half half) {
        final Door door = mock(Door.class);
        when(door.getMaterial()).thenReturn(Material.OAK_DOOR);
        when(door.getHalf()).thenReturn(half);
        return door;
    }

    private static BlockData data(Material material) {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        when(data.clone()).thenReturn(data);
        return data;
    }
}
