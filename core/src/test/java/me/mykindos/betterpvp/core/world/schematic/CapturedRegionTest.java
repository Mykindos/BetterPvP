package me.mykindos.betterpvp.core.world.schematic;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapturedRegionTest {

    private static final double EPSILON = 1e-6;

    private final World world = mock(World.class);

    /**
     * Mapper's region classes log through Bukkit in their static initialiser, so a server has to exist before any of
     * them is touched or the class fails to initialise.
     */
    @BeforeAll
    static void installServer() {
        if (Bukkit.getServer() != null) {
            return;
        }
        final Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger("captured-region-test"));
        Bukkit.setServer(server);
    }

    private Location at(double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    private Location at(double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static RegionOptions tagged(String... tags) {
        return RegionOptions.builder().tags(new java.util.LinkedHashSet<>(Set.of(tags))).build();
    }

    @Test
    @DisplayName("capturing then rebuilding at the same spot with no rotation reproduces the original position")
    void roundTripAtSameAnchor() {
        final Location anchor = at(100, 64, 200);
        final PointRegion original = new PointRegion("helm", at(103.5, 66.0, 197.5));

        final CapturedRegion captured = CapturedRegion.capture(original, anchor);
        final Region rebuilt = captured.rebuild(anchor, 0);

        final Location location = ((PointRegion) rebuilt).getLocation();
        assertEquals(103.5, location.getX(), EPSILON);
        assertEquals(66.0, location.getY(), EPSILON);
        assertEquals(197.5, location.getZ(), EPSILON);
    }

    @Test
    @DisplayName("rebuilding at a different anchor moves the region by the same offset")
    void rebuildTranslates() {
        final Location anchor = at(0, 64, 0);
        final PointRegion original = new PointRegion("helm", at(2.5, 65.0, -3.5));

        final CapturedRegion captured = CapturedRegion.capture(original, anchor);
        final Location moved = ((PointRegion) captured.rebuild(at(1000, 70, -1000), 0)).getLocation();

        assertEquals(1002.5, moved.getX(), EPSILON);
        assertEquals(71.0, moved.getY(), EPSILON);
        assertEquals(-1003.5, moved.getZ(), EPSILON);
    }

    /**
     * The reason the whole thing exists: a marker on a block must still be on that block after the hull is turned to
     * face a differently-oriented berth.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("a rotated marker lands on the block that block rotation moved its block to")
    void rotationKeepsMarkerOnItsBlock(int quarterTurns) {
        final Location anchor = at(0, 64, 0);
        // A marker at the centre of the block 3 east and 5 south of the anchor.
        final PointRegion original = new PointRegion("helm", at(3.5, 64.0, 5.5));

        final CapturedRegion captured = CapturedRegion.capture(original, anchor);
        final Location placed = ((PointRegion) captured.rebuild(anchor, quarterTurns)).getLocation();

        final int[] expectedBlock = SchematicAnimator.rotateXZ(3, 5, quarterTurns);
        assertEquals(expectedBlock[0] + 0.5, placed.getX(), EPSILON);
        assertEquals(expectedBlock[1] + 0.5, placed.getZ(), EPSILON);
    }

    @Test
    @DisplayName("a perspective marker's facing rotates with the structure")
    void perspectiveFacingRotates() {
        final Location anchor = at(0, 64, 0);
        final PerspectiveRegion original = new PerspectiveRegion("board", at(1.5, 64.0, 1.5, 0f, 10f));

        final CapturedRegion captured = CapturedRegion.capture(original, anchor);
        final Location placed = ((PointRegion) captured.rebuild(anchor, 1)).getLocation();

        assertEquals(270f, placed.getYaw(), 0.001f);
        assertEquals(10f, placed.getPitch(), 0.001f, "pitch is unaffected by turning about the vertical axis");
    }

    @Test
    @DisplayName("tags survive capture and rebuild, so capacity travels with the hull")
    void tagsSurvive() {
        final Location anchor = at(0, 64, 0);
        final PointRegion original = new PointRegion("ship", at(1, 64, 1), tagged("capacity:6", "id:galleon"));

        final Region rebuilt = CapturedRegion.capture(original, anchor).rebuild(anchor, 2);

        assertTrue(rebuilt.getOptions().getTags().contains("capacity:6"));
        assertTrue(rebuilt.getOptions().getTags().contains("id:galleon"));
    }

    @Test
    @DisplayName("name and type survive, so the content pipeline still recognises the data-point")
    void nameAndTypeSurvive() {
        final Location anchor = at(0, 64, 0);
        final PerspectiveRegion original = new PerspectiveRegion("prop", at(1, 64, 1));

        final Region rebuilt = CapturedRegion.capture(original, anchor).rebuild(anchor, 3);

        assertEquals("prop", rebuilt.getName());
        assertEquals(Region.RegionType.PERSPECTIVE, rebuilt.getType());
    }

    @Test
    @DisplayName("a cuboid keeps its size through a rotation")
    void cuboidKeepsVolume() {
        final Location anchor = at(0, 64, 0);
        final CuboidRegion original = new CuboidRegion("hull", at(0, 64, 0), at(9, 68, 4));

        final CuboidRegion rebuilt = (CuboidRegion) CapturedRegion.capture(original, anchor).rebuild(anchor, 1);

        // A quarter turn swaps the horizontal extents.
        final double width = rebuilt.getMax().getX() - rebuilt.getMin().getX();
        final double depth = rebuilt.getMax().getZ() - rebuilt.getMin().getZ();
        assertEquals(4, width, EPSILON);
        assertEquals(9, depth, EPSILON);
        assertEquals(4, rebuilt.getMax().getY() - rebuilt.getMin().getY(), EPSILON);
    }

    @Test
    @DisplayName("a path keeps its waypoints in order")
    void pathKeepsOrder() {
        final Location anchor = at(0, 64, 0);
        final PathRegion original = new PathRegion("npc_route",
                List.of(at(1, 64, 1), at(5, 64, 1), at(5, 64, 9)));

        final PathRegion rebuilt = (PathRegion) CapturedRegion.capture(original, anchor).rebuild(anchor, 0);

        assertEquals(3, rebuilt.size());
        assertEquals(1, rebuilt.getPoints().get(0).getX(), EPSILON);
        assertEquals(5, rebuilt.getPoints().get(1).getX(), EPSILON);
        assertEquals(9, rebuilt.getPoints().get(2).getZ(), EPSILON);
    }

    @Test
    @DisplayName("four quarter turns return every point to where it started")
    void fourTurnsRestoresPosition() {
        final Location anchor = at(0, 64, 0);
        final PointRegion original = new PointRegion("helm", at(7.5, 64.0, -2.5));
        final CapturedRegion captured = CapturedRegion.capture(original, anchor);

        Location placed = ((PointRegion) captured.rebuild(anchor, 4)).getLocation();

        assertEquals(7.5, placed.getX(), EPSILON);
        assertEquals(-2.5, placed.getZ(), EPSILON);
    }
}
