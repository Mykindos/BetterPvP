package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SightingGeometryTest {

    private static final double PIN = 100;
    private static final double ARRIVAL = 100;
    private static final double RAMP_START = 200;
    private static final float MIN_SCALE = 10f;
    private static final float MAX_SCALE = 26f;

    /** Halfway along the ramp, which is the one point in the middle worth naming. */
    private static final float MID_SCALE = (MIN_SCALE + MAX_SCALE) / 2f;

    private static float scale(double trueDistance) {
        return SightingGeometry.scale(trueDistance, ARRIVAL, RAMP_START, MIN_SCALE, MAX_SCALE);
    }

    @Test
    @DisplayName("an island is drawn at the pin distance however far away it really is")
    void everythingIsDrawnAtThePin() {
        for (double distance : new double[]{100, 250, 600, 1200, 1499}) {
            final OceanOffset pinned = SightingGeometry.pinned(new OceanOffset(0, distance), PIN);
            assertEquals(PIN, pinned.distance(), 1e-9, "drawn at " + pinned.distance() + " from " + distance);
        }
    }

    @Test
    @DisplayName("pinning keeps the true bearing, whatever quarter it is in")
    void pinningKeepsTheBearing() {
        for (int bearing = -170; bearing <= 180; bearing += 10) {
            final double radians = Math.toRadians(bearing);
            final OceanOffset out = new OceanOffset(Math.sin(radians) * 830, Math.cos(radians) * 830);
            final OceanOffset pinned = SightingGeometry.pinned(out, PIN);

            assertEquals(out.bearing(), pinned.bearing(), 1e-9, "bearing moved at " + bearing);
            assertEquals(PIN, pinned.distance(), 1e-9);
        }
    }

    @Test
    @DisplayName("an island dead on top of the ship is still given a bearing rather than a zero-length one")
    void aZeroOffsetStillHasSomewhereToGo() {
        final OceanOffset pinned = SightingGeometry.pinned(new OceanOffset(0, 0), PIN);

        assertEquals(PIN, pinned.distance(), 1e-9);
        assertEquals(0, pinned.bearing(), 1e-9);
    }

    @Test
    @DisplayName("the icon is at its smallest until the ramp starts")
    void distantIslandsAreSmall() {
        assertEquals(MIN_SCALE, scale(RAMP_START), 1e-6);
        assertEquals(MIN_SCALE, scale(600), 1e-6);
        assertEquals(MIN_SCALE, scale(1500), 1e-6);
    }

    @Test
    @DisplayName("the icon is at its largest by the time the crew arrives, and no larger inside that")
    void arrivalIsFullSize() {
        assertEquals(MAX_SCALE, scale(ARRIVAL), 1e-6);
        assertEquals(MAX_SCALE, scale(40), 1e-6);
        assertEquals(MAX_SCALE, scale(0), 1e-6);
    }

    @Test
    @DisplayName("the ramp only ever grows as the island closes")
    void theRampIsMonotonic() {
        float previous = scale(RAMP_START);
        for (double distance = RAMP_START; distance >= ARRIVAL; distance -= 1) {
            final float current = scale(distance);
            assertTrue(current >= previous, "the icon shrank at " + distance);
            previous = current;
        }
        assertEquals(MID_SCALE, scale(150), 1e-6);
    }

    @Test
    @DisplayName("a ramp with no room to run leaves the icon at its smallest rather than dividing by zero")
    void degenerateRampDoesNotGrow() {
        assertEquals(MIN_SCALE, SightingGeometry.scale(100, 200, 200, MIN_SCALE, MAX_SCALE), 1e-6);
        assertEquals(MIN_SCALE, SightingGeometry.scale(100, 200, 150, MIN_SCALE, MAX_SCALE), 1e-6);
    }
}
