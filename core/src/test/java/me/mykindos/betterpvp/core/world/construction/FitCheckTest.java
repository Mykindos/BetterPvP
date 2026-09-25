package me.mykindos.betterpvp.core.world.construction;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A 2-block-long structure, checked against build zones that cover x from 0 to 9 (and near water only below x 5). */
class FitCheckTest {

    private final ZoneManager zones = mock(ZoneManager.class);
    private final StructureShapes shapes = mock(StructureShapes.class);
    private final FitCheck fitCheck = new FitCheck(zones, shapes);
    private final Holding holding = new Holding();
    private World world;
    private Schematic twoLong;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        final Zone plain = zone(Set.of(FitCheck.BUILD_ZONE));
        final Zone waterside = zone(Set.of(FitCheck.BUILD_ZONE, "near_water"));
        when(zones.getZonesAt(any())).thenAnswer(invocation -> {
            final Location at = invocation.getArgument(0);
            if (at.getX() < 0 || at.getX() >= 10) {
                return List.of();
            }
            return at.getX() < 5 ? List.of(waterside) : List.of(plain);
        });

        final BlockData stone = mock(BlockData.class);
        when(stone.getMaterial()).thenReturn(Material.STONE);
        twoLong = new Schematic(2, 1, 1, List.of(new Schematic.PlacedBlock(0, 0, 0, stone),
                new Schematic.PlacedBlock(1, 0, 0, stone)));
    }

    @Test
    void itFitsInsideABuildZone() {
        assertTrue(fitCheck.problem(world, holding, type(null), at(2), null).isEmpty());
    }

    @Test
    void itMustNotHangOutsideTheBuildZones() {
        assertFalse(fitCheck.problem(world, holding, type(null), at(9), null).isEmpty());
    }

    @Test
    void itMayStraddleTwoBuildZones() {
        assertTrue(fitCheck.problem(world, holding, type(null), at(4), null).isEmpty());
    }

    @Test
    void aTaggedStructureNeedsItsTagUnderEveryColumn() {
        assertTrue(fitCheck.problem(world, holding, type("near_water"), at(2), null).isEmpty());
        assertFalse(fitCheck.problem(world, holding, type("near_water"), at(4), null).isEmpty());
    }

    @Test
    void itMustKeepItsDistanceFromAnotherStructureUnlessItIsThatStructure() {
        final PlacedStructure other = standingAt(9);

        assertFalse(fitCheck.problem(world, holding, type(null), at(3), null).isEmpty(),
                "4 blocks of ground between the two is too close");
        assertTrue(fitCheck.problem(world, holding, type(null), at(3), other.getId()).isEmpty());
        assertTrue(fitCheck.problem(world, holding, type(null), at(2), null).isEmpty(),
                "5 blocks of ground between the two is far enough");
    }

    @Test
    void clashesAreTheColumnsOutsideTheZonesOrTooCloseToAnotherStructure() {
        assertTrue(fitCheck.clashes(world, holding, type(null), at(2), null).isEmpty());
        assertEquals(LongSet.of(Footprint.pack(10, 0)), fitCheck.clashes(world, holding, type(null), at(9), null));

        final PlacedStructure other = standingAt(9);

        assertEquals(LongSet.of(Footprint.pack(4, 0)), fitCheck.clashes(world, holding, type(null), at(3), null));
        assertTrue(fitCheck.clashes(world, holding, type(null), at(3), other.getId()).isEmpty());
        assertTrue(fitCheck.clashes(world, holding, type(null), at(2), null).isEmpty());
    }

    private PlacedStructure standingAt(int x) {
        final PlacedStructure other = new PlacedStructure(UUID.randomUUID(), "hall",
                new StructurePosition(x, 64, 0, 0), StructureCondition.ACTIVE);
        holding.getStructures().add(other);
        when(shapes.boundsOf(any(), anyString(), anyInt(), any()))
                .thenAnswer(invocation -> Optional.of(at(x).selectionBounds()));
        return other;
    }

    private SchematicPlacement at(int x) {
        return SchematicPlacement.of(twoLong, new Location(world, x, 64, 0), 0);
    }

    private static Zone zone(Set<String> tags) {
        final Zone zone = mock(Zone.class);
        when(zone.hasTag(anyString())).thenAnswer(invocation -> tags.contains(invocation.<String>getArgument(0)));
        return zone;
    }

    private static StructureType type(String requiredTag) {
        return new StructureType() {
            @Override
            public @NotNull String getId() {
                return "test";
            }

            @Override
            public @NotNull Component getDisplayName() {
                return Component.text("Test");
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
                return requiredTag;
            }

            @Override
            public @NotNull List<StructureStage> getStages() {
                return List.of(new StructureStage("test", ResourceCost.NONE, Duration.ZERO));
            }

            @Override
            public @NotNull StructureFlags getFlags() {
                return StructureFlags.builder().build();
            }
        };
    }
}
