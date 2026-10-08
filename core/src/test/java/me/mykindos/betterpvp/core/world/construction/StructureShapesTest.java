package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructureShapesTest {

    private static final StructurePosition HERE = new StructurePosition(0, 64, 0, 0);

    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final SchematicService schematics = mock(SchematicService.class);
    private final World world = mock(World.class);
    private final StructureShapes shapes = new StructureShapes(catalogue, schematics);

    @Test
    void ac1_aReloadedBuildIsMeasuredAgain() {
        final Schematic one = longBy(1);
        final Schematic three = longBy(3);
        catalogue.register(type());
        when(schematics.generation()).thenReturn(0L);
        when(schematics.load("hall")).thenReturn(Optional.of(one));
        final BoundingBox before = shapes.boundsOf(world, "hall", 0, HERE).orElseThrow();
        assertEquals(1, shapes.footprintOf(world, "hall", 0, HERE).orElseThrow().getColumns().size());

        when(schematics.generation()).thenReturn(1L);
        when(schematics.load("hall")).thenReturn(Optional.of(three));

        assertEquals(before.getWidthX() + 2, shapes.boundsOf(world, "hall", 0, HERE).orElseThrow().getWidthX(), 1e-9);
        assertEquals(3, shapes.footprintOf(world, "hall", 0, HERE).orElseThrow().getColumns().size());
    }

    @Test
    void ac1_withoutAReloadTheMeasureIsKept() {
        final Schematic one = longBy(1);
        final Schematic three = longBy(3);
        catalogue.register(type());
        when(schematics.generation()).thenReturn(0L);
        when(schematics.load("hall")).thenReturn(Optional.of(one));
        final BoundingBox before = shapes.boundsOf(world, "hall", 0, HERE).orElseThrow();

        when(schematics.load("hall")).thenReturn(Optional.of(three));

        assertEquals(before.getWidthX(), shapes.boundsOf(world, "hall", 0, HERE).orElseThrow().getWidthX(), 1e-9);
    }

    /** A build one block deep and {@code blocks} long along x. */
    private static Schematic longBy(int blocks) {
        final BlockData stone = mock(BlockData.class);
        when(stone.getMaterial()).thenReturn(Material.STONE);
        final List<Schematic.PlacedBlock> placed = new ArrayList<>();
        for (int x = 0; x < blocks; x++) {
            placed.add(new Schematic.PlacedBlock(x, 0, 0, stone));
        }
        return new Schematic(blocks, 1, 1, placed);
    }

    private static StructureType type() {
        return new StructureType() {
            @Override
            public @NotNull String getId() {
                return "hall";
            }

            @Override
            public @NotNull Component getDisplayName() {
                return Component.text("Hall");
            }

            @Override
            public int getTier() {
                return 1;
            }

            @Override
            public @NotNull Set<String> getRequiredStructures() {
                return Set.of();
            }

            @Override
            public String getRequiredZoneTag() {
                return null;
            }

            @Override
            public @NotNull List<StructureStage> getStages() {
                return List.of(new StructureStage("hall", ResourceCost.NONE, Duration.ZERO));
            }

            @Override
            public @NotNull StructureFlags getFlags() {
                return StructureFlags.builder().build();
            }
        };
    }
}
