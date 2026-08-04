package me.mykindos.betterpvp.clans.world.discovery.hazard;

import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardGeometryTest {

    /** A ten by thirty hull sitting on the origin, deliberately taller than it is wide so height cannot be ignored by luck. */
    private static final BoundingBox HULL = new BoundingBox(-5, 60, -15, 5, 70, 15);

    @Test
    @DisplayName("a hazard just outside the expanded hull is a miss and just inside is a hit")
    void reachIsTheHullPlusTheRadius() {
        assertFalse(HazardGeometry.strikes(HULL, 11.01, 0, 6), "a whisker beyond the reach");
        assertTrue(HazardGeometry.strikes(HULL, 10.99, 0, 6), "a whisker inside it");
    }

    @Test
    @DisplayName("the radius is what the reach is expanded by, so a wider hazard strikes from further out")
    void radiusWidensTheReach() {
        assertFalse(HazardGeometry.strikes(HULL, 8, 0, 2));
        assertTrue(HazardGeometry.strikes(HULL, 8, 0, 4));
    }

    @Test
    @DisplayName("the reach expands along the hull's length as well as its beam")
    void reachCoversBothAxes() {
        assertTrue(HazardGeometry.strikes(HULL, 0, 20, 6));
        assertFalse(HazardGeometry.strikes(HULL, 0, 22, 6));
    }

    @Test
    @DisplayName("a point on top of the hull is a hit whatever the height of the box")
    void heightIsNotPartOfIt() {
        assertTrue(HazardGeometry.strikes(HULL, 0, 0, 0));
    }

    @Test
    @DisplayName("the hull it is given is never modified, so a tick's tests do not stack")
    void hullIsLeftAlone() {
        final BoundingBox hull = HULL.clone();
        HazardGeometry.strikes(hull, 0, 0, 25);

        assertTrue(hull.equals(HULL), "expanding for one test must not widen the hull for the next");
    }

    @Test
    @DisplayName("only what is further out than the radius has fallen off the sea")
    void beyondIsStrictlyFurtherOut() {
        assertFalse(HazardGeometry.beyond(119.9, 120));
        assertFalse(HazardGeometry.beyond(120, 120), "exactly on the horizon is still in sight");
        assertTrue(HazardGeometry.beyond(120.1, 120));
    }
}
