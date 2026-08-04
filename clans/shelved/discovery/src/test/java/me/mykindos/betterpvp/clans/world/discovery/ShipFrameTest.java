package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipFrameTest {

    @Test
    @DisplayName("an offset dead ahead of a hull facing +Z lands further along +Z")
    void aheadOfNorthIsPositiveZ() {
        final ShipFrame frame = ShipFrame.of(100, 200, 0);

        final OceanOffset ahead = new OceanOffset(0, 30);

        assertEquals(100, frame.worldX(ahead), 1e-9);
        assertEquals(230, frame.worldZ(ahead), 1e-9);
    }

    @Test
    @DisplayName("starboard of a hull facing +Z lands at -X, matching the yaw convention")
    void starboardOfNorthIsNegativeX() {
        final ShipFrame frame = ShipFrame.of(100, 200, 0);

        final OceanOffset abeam = new OceanOffset(30, 0);

        assertEquals(70, frame.worldX(abeam), 1e-9);
        assertEquals(200, frame.worldZ(abeam), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({
            "0,     0,   40,   0,   40",
            "90,    0,   40, -40,    0",
            "180,   0,   40,   0,  -40",
            "270,   0,   40,  40,    0",
            "90,   40,    0,   0,  -40",
            "180,  40,    0,  40,    0",
    })
    @DisplayName("a turned hull carries its offsets round with it")
    void yawRotatesTheOffset(double yaw, double right, double forward, double expectedX, double expectedZ) {
        final ShipFrame frame = ShipFrame.of(0, 0, yaw);

        final OceanOffset offset = new OceanOffset(right, forward);

        assertEquals(expectedX, frame.worldX(offset), 1e-9);
        assertEquals(expectedZ, frame.worldZ(offset), 1e-9);
    }

    /**
     * The frame's axes have to agree with the ones {@link ShipDynamics} publishes, because the two are read as the same
     * convention at opposite ends of the feature — the virtual heading drives motion, the hull's yaw draws the result.
     */
    @ParameterizedTest
    @CsvSource({"0", "37", "90", "153", "180", "271", "359"})
    @DisplayName("the frame's axes match the dynamics' axes for the same angle")
    void axesMatchDynamics(double angle) {
        final ShipFrame frame = ShipFrame.of(0, 0, angle);
        final ShipDynamics dynamics = new ShipDynamics(ShipDynamicsConfig.DEFAULT, angle);

        assertEquals(dynamics.forwardX(), frame.forwardX(), 1e-12);
        assertEquals(dynamics.forwardZ(), frame.forwardZ(), 1e-12);
        assertEquals(dynamics.rightX(), frame.rightX(), 1e-12);
        assertEquals(dynamics.rightZ(), frame.rightZ(), 1e-12);
    }

    /**
     * The whole pipeline: a point on the virtual plane is projected into the ship's frame by the ocean and then laid
     * back down beside the real hull. Its distance and bearing from the hull must survive that, whatever the virtual
     * heading is doing — the virtual rotation belongs to the ocean and must not be applied twice.
     */
    @ParameterizedTest
    @CsvSource({"0", "45", "120", "200", "310"})
    @DisplayName("a point projected through the ocean keeps its range when drawn beside the hull")
    void oceanOffsetSurvivesTheRoundTrip(double virtualHeading) {
        final Ocean ocean = new Ocean(1200, -450);
        final ShipFrame frame = ShipFrame.of(64, -16, 130);

        final OceanPoint point = ocean.pointAt(virtualHeading, 25, 90);
        final OceanOffset offset = ocean.localOf(point, virtualHeading);

        assertEquals(90, offset.distance(), 1e-6, "the ocean's own projection must be range-preserving");
        assertEquals(25, offset.bearing(), 1e-6);

        final double dx = frame.worldX(offset) - frame.getOriginX();
        final double dz = frame.worldZ(offset) - frame.getOriginZ();
        assertEquals(90, Math.hypot(dx, dz), 1e-6, "the same range, measured from the real hull");
    }

    /**
     * A point 25 degrees off the bow has to be drawn 25 degrees off the <em>hull's</em> bow, not off world north — that
     * is the one thing that goes wrong if the anchor's yaw is dropped.
     */
    @Test
    @DisplayName("a bearing off the bow is drawn relative to the hull's own facing")
    void bearingIsRelativeToTheHull() {
        final ShipFrame frame = ShipFrame.of(0, 0, 130);

        final OceanOffset offBow = new OceanOffset(Math.sin(Math.toRadians(25)) * 90,
                Math.cos(Math.toRadians(25)) * 90);

        final double worldBearing = Math.toDegrees(Math.atan2(
                -frame.worldX(offBow), frame.worldZ(offBow)));
        // The hull faces 130 degrees, so its bow bearing plus the 25 degrees to starboard is 155 in world terms.
        assertEquals(155, ShipDynamics.normalise(worldBearing), 1e-6);
    }
}
