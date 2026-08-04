package me.mykindos.betterpvp.clans.world.ship;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BerthTest {

    private static final String WORLD = "Season-2/Spawn";

    private final World world = world(WORLD);

    private static World world(String name) {
        final World mocked = mock(World.class);
        when(mocked.getName()).thenReturn(name);
        return mocked;
    }

    private Berth berth(BoundingBox hull) {
        final Location anchor = new Location(world, 100, 64, 100);
        return new Berth("north_berth", WORLD, "galleon", anchor, anchor, hull, 6, null);
    }

    private static BoundingBox hull() {
        return new BoundingBox(96, 60, 96, 108, 72, 108);
    }

    @Test
    @DisplayName("a location inside the hull is aboard")
    void insideHullIsAboard() {
        assertTrue(berth(hull()).contains(new Location(world, 100, 65, 100)));
    }

    @Test
    @DisplayName("a location outside the hull is not aboard")
    void outsideHullIsAshore() {
        assertFalse(berth(hull()).contains(new Location(world, 500, 64, 500)));
    }

    /**
     * The bounds are numbers, not a world-bound region, so nothing in them says which world they belong to. Two docks
     * cloned from one island template hold hulls at identical coordinates — without the world check, standing on one
     * would make you aboard the other.
     */
    @Test
    @DisplayName("the same coordinates in another world are not aboard")
    void otherWorldIsNotAboard() {
        assertFalse(berth(hull()).contains(new Location(world("islands/mining/a1b2c3d4"), 100, 65, 100)));
    }

    /**
     * An empty mooring must leave the ship un-crewable, not crewable from everywhere — the latter would silently let
     * anyone anywhere in the world join a crew moored at this berth.
     */
    @Test
    @DisplayName("a berth with no ship holds nobody rather than everybody")
    void missingHullContainsNobody() {
        final Berth empty = berth(null);

        assertFalse(empty.isCrewable());
        assertFalse(empty.contains(new Location(world, 100, 64, 100)),
                "even the berth's own spot is not aboard a ship with no bounds");
    }

    @Test
    @DisplayName("a moored ship is crewable")
    void hullMakesItCrewable() {
        assertTrue(berth(hull()).isCrewable());
    }
}
