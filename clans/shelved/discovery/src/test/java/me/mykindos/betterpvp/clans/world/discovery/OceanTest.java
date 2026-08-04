package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanTest {

    @ParameterizedTest
    @CsvSource({
            "0,     0,   10",
            "90,  -10,    0",
            "180,   0,  -10",
            "270,  10,    0",
    })
    @DisplayName("advancing runs along the heading's forward vector")
    void advanceFollowsHeading(double heading, double expectedX, double expectedZ) {
        final Ocean ocean = new Ocean(0, 0);

        ocean.advance(2.0, 5.0, heading);

        assertEquals(expectedX, ocean.getX(), 1e-9);
        assertEquals(expectedZ, ocean.getZ(), 1e-9);
    }

    @Test
    @DisplayName("a point dead ahead is all forward and no sideways")
    void deadAheadIsPureForward() {
        final Ocean ocean = new Ocean(0, 0);

        final OceanOffset offset = ocean.localOf(0, 40, 0);

        assertEquals(40, offset.getForward(), 1e-9);
        assertEquals(0, offset.getRight(), 1e-9);
        assertEquals(40, offset.distance(), 1e-9);
        assertEquals(0, offset.bearing(), 1e-9);
    }

    @Test
    @DisplayName("a point abeam to starboard of a ship facing +Z sits at -X")
    void starboardOfNorthIsNegativeX() {
        final Ocean ocean = new Ocean(0, 0);

        final OceanOffset offset = ocean.localOf(-40, 0, 0);

        assertEquals(40, offset.getRight(), 1e-9);
        assertEquals(0, offset.getForward(), 1e-9);
        assertEquals(90, offset.bearing(), 1e-9);
    }

    /** The transform the whole feature rests on: it has to survive the round trip at awkward headings too. */
    @ParameterizedTest
    @CsvSource({
            "0,      0,    50",
            "0,     35,   120",
            "0,    -35,   120",
            "90,    90,    75",
            "137.5, -90,   75",
            "212,   180,   60",
            "349,  -170,  200",
    })
    @DisplayName("a point placed at a bearing and range reads back at the same bearing and range")
    void pointAtRoundTrips(double heading, double bearing, double distance) {
        final Ocean ocean = new Ocean(-812.25, 3390.5);

        final OceanPoint point = ocean.pointAt(heading, bearing, distance);
        final OceanOffset offset = ocean.localOf(point, heading);

        assertEquals(distance, offset.distance(), 1e-9);
        assertEquals(bearing, offset.bearing(), 1e-9);
    }

    @Test
    @DisplayName("turning to starboard slides a point that was dead ahead over to port")
    void turningStarboardMovesAheadPointToPort() {
        final Ocean ocean = new Ocean(0, 0);
        final OceanPoint ahead = ocean.pointAt(0, 0, 100);

        assertEquals(0, ocean.localOf(ahead, 0).getRight(), 1e-9);

        final OceanOffset afterTurn = ocean.localOf(ahead, 30);

        assertTrue(afterTurn.getRight() < 0, "the point should now lie off the port bow: " + afterTurn.getRight());
        assertEquals(-30, afterTurn.bearing(), 1e-9);
        assertEquals(100, afterTurn.distance(), 1e-9);
    }

    @Test
    @DisplayName("sailing past a point puts it astern")
    void sailingPastAPointPutsItAstern() {
        final Ocean ocean = new Ocean(0, 0);
        final OceanPoint marker = ocean.pointAt(0, 0, 30);

        ocean.advance(10, 8, 0);

        final OceanOffset offset = ocean.localOf(marker, 0);
        assertEquals(-50, offset.getForward(), 1e-9);
        assertEquals(180, offset.bearing(), 1e-9);
    }

    @Test
    @DisplayName("bearing stays in (-180, 180] rather than wrapping to a second name for astern")
    void bearingRangeIsHalfOpen() {
        assertEquals(180, new OceanOffset(0, -10).bearing(), 1e-9);
        assertEquals(-90, new OceanOffset(-10, 0).bearing(), 1e-9);
        assertEquals(45, new OceanOffset(10, 10).bearing(), 1e-9);
    }

    @Test
    @DisplayName("the two localOf overloads agree")
    void overloadsAgree() {
        final Ocean ocean = new Ocean(120, -60);
        final OceanPoint point = new OceanPoint(45, 900);

        assertEquals(ocean.localOf(45, 900, 62), ocean.localOf(point, 62));
    }
}
