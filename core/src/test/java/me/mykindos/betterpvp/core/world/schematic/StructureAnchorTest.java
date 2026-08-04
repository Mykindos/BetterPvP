package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The anchor decides where every copy of a structure lands and which way it faces, so the rules for finding it are
 * worth pinning: a ship anchored to the wrong marker floats at the wrong depth in every world it is moored in.
 */
class StructureAnchorTest {

    private final World world = mock(World.class);

    @BeforeAll
    static void installServer() {
        if (Bukkit.getServer() != null) {
            return;
        }
        final Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger("structure-anchor-test"));
        Bukkit.setServer(server);
    }

    private Location at(double x, double y, double z, float yaw) {
        return new Location(world, x, y, z, yaw, 0f);
    }

    private Region anchor(double x, double y, double z, float yaw) {
        return new PerspectiveRegion(StructureAnchor.POINT, at(x, y, z, yaw));
    }

    @Test
    @DisplayName("the anchor marker is found among the structure's data-points")
    void findsTheAnchor() {
        final Optional<Location> found = StructureAnchor.find(List.of(
                new PerspectiveRegion("prop", at(5, 70, 5, 0f)),
                anchor(10, 63, 20, 90f),
                new PerspectiveRegion("npc_resident", at(6, 70, 6, 0f))));

        assertTrue(found.isPresent());
        assertEquals(10, found.get().getX(), 1e-9);
        assertEquals(63, found.get().getY(), 1e-9);
        assertEquals(90f, found.get().getYaw(), 0.001f);
    }

    @Test
    @DisplayName("a structure with no anchor reports none, so the caller can fall back and say so")
    void missingAnchorIsEmpty() {
        assertTrue(StructureAnchor.find(List.of(
                new PerspectiveRegion("prop", at(1, 1, 1, 0f)))).isEmpty());
        assertTrue(StructureAnchor.find(List.of()).isEmpty());
    }

    /**
     * Facing is half the anchor's job — rotation is measured from it — and a plain point has none, so one authored with
     * the wrong wand must not be accepted as an anchor.
     */
    @Test
    @DisplayName("an anchor authored as a plain point is not used, since it carries no facing")
    void anchorMustBeAPerspective() {
        assertTrue(StructureAnchor.find(List.of(
                new PointRegion(StructureAnchor.POINT, at(3, 3, 3, 0f)))).isEmpty());
    }

    @Test
    @DisplayName("isAnchored distinguishes an authored origin from the builder's position")
    void isAnchoredReflectsTheMarker() {
        final CapturedRegion anchorRegion = CapturedRegion.builder()
                .name(StructureAnchor.POINT)
                .type(Region.RegionType.PERSPECTIVE)
                .tags(Set.of())
                .points(List.of(CapturedRegion.RelativePoint.builder().x(0).y(0).z(0).yaw(0f).pitch(0f).build()))
                .build();

        assertTrue(StructureAnchor.isAnchored(
                new Schematic(1, 1, 1, 0, 0, 0, List.of(), List.of(anchorRegion), 0f)));
        assertFalse(StructureAnchor.isAnchored(new Schematic(1, 1, 1, List.of())));
    }

    /**
     * The property the anchor exists for: whatever is at the anchor lands exactly on the marker it is pasted at, so a
     * stern authored at the waterline sits at the waterline wherever it is moored.
     */
    @Test
    @DisplayName("the anchor point itself lands exactly on the berth marker")
    void anchorLandsOnTheBerthMarker() {
        final Location anchorSpot = at(140, 63, -22, 180f);
        final CapturedRegion captured = CapturedRegion.capture(
                new PerspectiveRegion(StructureAnchor.POINT, anchorSpot), anchorSpot);

        // Captured relative to itself, so it sits at the structure's origin.
        assertEquals(0, captured.getPoints().getFirst().getX(), 1e-9);
        assertEquals(0, captured.getPoints().getFirst().getY(), 1e-9);
        assertEquals(0, captured.getPoints().getFirst().getZ(), 1e-9);

        final Location berth = at(1000, 65, 1000, 180f);
        final Location placed = ((PointRegion) captured.rebuild(berth, 0)).getLocation();

        assertEquals(1000, placed.getX(), 1e-9);
        assertEquals(65, placed.getY(), 1e-9, "the ship floats at the berth marker's height");
        assertEquals(1000, placed.getZ(), 1e-9);
    }
}
