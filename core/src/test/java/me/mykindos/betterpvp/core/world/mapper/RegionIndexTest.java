package me.mykindos.betterpvp.core.world.mapper;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PathRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.region.RegionOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionIndexTest {

    private final World world = mock(World.class);
    private final World otherWorld = mock(World.class);

    @BeforeAll
    static void installServer() {
        if (Bukkit.getServer() != null) {
            return;
        }
        final Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger("region-index-test"));
        Bukkit.setServer(server);
    }

    private Location at(World in, double x, double y, double z) {
        return new Location(in, x, y, z);
    }

    private static RegionOptions tagged(String... tags) {
        return RegionOptions.builder().tags(new LinkedHashSet<>(Set.of(tags))).build();
    }

    @Test
    @DisplayName("find returns only regions of the requested name and type")
    void findFiltersByNameAndType() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new PerspectiveRegion("prop", at(otherWorld, 1, 1, 1)),
                new PerspectiveRegion("prop", at(otherWorld, 2, 2, 2)),
                new PerspectiveRegion("npc_resident", at(otherWorld, 3, 3, 3)),
                new PointRegion("prop", at(otherWorld, 4, 4, 4))));

        assertEquals(2, index.find("prop", PerspectiveRegion.class).size(),
                "the point authored as 'prop' is the wrong type and must be dropped");
        assertEquals(1, index.find("npc_resident", PerspectiveRegion.class).size());
    }

    @Test
    @DisplayName("data-point names match regardless of case")
    void findIsCaseInsensitive() {
        final RegionIndex index = RegionIndex.of(world, List.of(new PointRegion("Ship_Berth", at(otherWorld, 1, 1, 1))));
        assertEquals(1, index.find("ship_berth", PointRegion.class).size());
    }

    /**
     * The bug this closes: content used to have to remember {@code setWorld} before reading a location, and forgetting
     * it is invisible on a one-world server and wrong the moment the same content runs anywhere else.
     */
    @Test
    @DisplayName("returned regions are bound to the index's world, not the one they were parsed with")
    void findBindsRegionsToTheIndexWorld() {
        final PointRegion region = new PointRegion("prop", at(otherWorld, 1, 1, 1));
        final RegionIndex index = RegionIndex.of(world, List.of(region));

        final PointRegion found = index.find("prop", PointRegion.class).getFirst();
        assertSame(world, found.getLocation().getWorld());
    }

    @Test
    @DisplayName("an absent data-point yields an empty list rather than throwing")
    void findMissingIsEmpty() {
        final RegionIndex index = RegionIndex.of(world, List.of());
        assertTrue(index.find("nothing_here", PointRegion.class).isEmpty());
        assertTrue(index.findOne("nothing_here", PointRegion.class).isEmpty());
    }

    @Test
    @DisplayName("findOne returns the first match in file order")
    void findOneTakesTheFirst() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new PointRegion("dock", at(otherWorld, 1, 1, 1), tagged("id:first")),
                new PointRegion("dock", at(otherWorld, 2, 2, 2), tagged("id:second"))));

        final PointRegion found = index.findOne("dock", PointRegion.class).orElseThrow();
        assertEquals(1, found.getLocation().getX(), 1e-9);
    }

    @Test
    @DisplayName("byId indexes on the id tag and skips untagged regions")
    void byIdIndexesTaggedRegionsOnly() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new PathRegion("npc_route", List.of(at(otherWorld, 0, 0, 0), at(otherWorld, 1, 0, 0)), tagged("id:market")),
                new PathRegion("npc_route", List.of(at(otherWorld, 0, 0, 0), at(otherWorld, 2, 0, 0)))));

        final Map<String, PathRegion> byId = index.byId("npc_route", PathRegion.class);
        assertEquals(1, byId.size());
        assertTrue(byId.containsKey("market"));
    }

    @Test
    @DisplayName("byId lower-cases ids so a lookup does not depend on how the tag was typed")
    void byIdIsCaseInsensitive() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new PathRegion("npc_route", List.of(at(otherWorld, 0, 0, 0), at(otherWorld, 1, 0, 0)), tagged("id:MarketLoop"))));

        assertTrue(index.byId("npc_route", PathRegion.class).containsKey("marketloop"));
    }

    /**
     * Duplicate ids are a validation error; the index still has to behave predictably when one slips through, and
     * first-wins is what the validators describe.
     */
    @Test
    @DisplayName("a duplicate id keeps the first holder")
    void byIdKeepsFirstOnDuplicate() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new CuboidRegion("ship", at(otherWorld, 0, 0, 0), at(otherWorld, 1, 1, 1), tagged("id:dup")),
                new CuboidRegion("ship", at(otherWorld, 5, 5, 5), at(otherWorld, 6, 6, 6), tagged("id:dup"))));

        final CuboidRegion kept = index.byId("ship", CuboidRegion.class).get("dup");
        assertEquals(0, kept.getMin().getX(), 1e-9);
    }

    @Test
    @DisplayName("regions contributed alongside authored ones are indistinguishable once indexed")
    void contributedRegionsMergeWithAuthoredOnes() {
        final Region authored = new PerspectiveRegion("prop", at(otherWorld, 1, 1, 1), tagged("id:dock_lamp"));
        final Region pasted = new PerspectiveRegion("prop", at(otherWorld, 20, 1, 20), tagged("id:helm"));

        final RegionIndex index = RegionIndex.of(world, List.of(authored, pasted));

        assertEquals(2, index.find("prop", PerspectiveRegion.class).size());
        assertTrue(index.byId("prop", PerspectiveRegion.class).containsKey("helm"));
    }

    @Test
    @DisplayName("all() exposes every region for callers that need to scan")
    void allExposesEverything() {
        final RegionIndex index = RegionIndex.of(world, List.of(
                new PointRegion("a", at(otherWorld, 1, 1, 1)),
                new PointRegion("b", at(otherWorld, 2, 2, 2))));

        assertEquals(2, index.all().size());
        assertFalse(index.all().isEmpty());
        assertSame(world, index.getWorld());
    }
}
