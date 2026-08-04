package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipDynamicsTest {

    private static ShipDynamics ship() {
        return new ShipDynamics(ShipDynamicsConfig.DEFAULT, 0);
    }

    private static ShipDynamics settled(int netInput) {
        final ShipDynamics ship = ship();
        for (int step = 0; step < 400; step++) {
            ship.advance(0.05, netInput);
        }
        return ship;
    }

    @Test
    @DisplayName("sustained input stops at the stops rather than winding past them")
    void rudderClampsAtOne() {
        assertEquals(1.0, settled(1).getRudder(), 1e-9);
        assertEquals(-1.0, settled(-1).getRudder(), 1e-9);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.05, 0.2, 1.0, 7.5})
    @DisplayName("releasing the wheel decays to exactly centre and stays there, whatever the step size")
    void releaseSettlesAtCentre(double dt) {
        final ShipDynamics ship = settled(1);

        for (int step = 0; step < 5000; step++) {
            ship.advance(dt, 0);
            assertTrue(ship.getRudder() >= 0, "the wheel crossed centre while releasing: " + ship.getRudder());
        }

        assertEquals(0.0, ship.getRudder(), 1e-12);
    }

    @Test
    @DisplayName("a wheel hard to port also decays up to exactly centre")
    void releaseFromPortSettlesAtCentre() {
        final ShipDynamics ship = settled(-1);

        for (int step = 0; step < 500; step++) {
            ship.advance(0.2, 0);
            assertTrue(ship.getRudder() <= 0, "the wheel crossed centre while releasing: " + ship.getRudder());
        }

        assertEquals(0.0, ship.getRudder(), 1e-12);
    }

    /** Holding both controls cancels out; it must not be a third, distinct behaviour. */
    @Test
    @DisplayName("both controls held behaves exactly like letting go")
    void bothHeldMatchesRelease() {
        final ShipDynamics released = settled(1);
        final ShipDynamics cancelled = settled(1);

        for (int step = 0; step < 40; step++) {
            released.advance(0.05, 0);
            cancelled.advance(0.05, 1 + -1);
        }

        assertEquals(released.getRudder(), cancelled.getRudder(), 1e-12);
        assertEquals(released.getHeading(), cancelled.getHeading(), 1e-12);
    }

    @Test
    @DisplayName("the wheel gains less near the stops than it does through centre")
    void resistanceSlowsTheWheelNearFullDeflection() {
        final ShipDynamics fromCentre = ship();
        fromCentre.advance(0.05, 1);
        final double gainAtCentre = fromCentre.getRudder();

        final ShipDynamics nearStop = ship();
        while (nearStop.getRudder() < 0.9) {
            nearStop.advance(0.01, 1);
        }
        final double before = nearStop.getRudder();
        nearStop.advance(0.05, 1);
        final double gainNearStop = nearStop.getRudder() - before;

        assertTrue(gainNearStop < gainAtCentre,
                "gain near the stop (" + gainNearStop + ") should be smaller than at centre (" + gainAtCentre + ")");
    }

    @Test
    @DisplayName("opposing input drags a settled starboard wheel through centre and over to port")
    void opposingInputCrossesCentre() {
        final ShipDynamics ship = settled(1);

        boolean sawCentre = false;
        for (int step = 0; step < 400; step++) {
            ship.advance(0.05, -1);
            if (Math.abs(ship.getRudder()) < 0.1) {
                sawCentre = true;
            }
        }

        assertTrue(sawCentre, "the wheel jumped sides without passing through centre");
        assertEquals(-1.0, ship.getRudder(), 1e-9);
    }

    @Test
    @DisplayName("the turn rate settles on the rudder's rate without exceeding it")
    void yawRateConvergesWithoutExceedingItsTarget() {
        final ShipDynamics ship = ship();

        for (int step = 0; step < 600; step++) {
            ship.advance(0.05, 1);
            assertTrue(ship.getYawRate() <= ShipDynamicsConfig.DEFAULT.getMaxYawRate() + 1e-9,
                    "turn rate overshot its target: " + ship.getYawRate());
        }

        assertEquals(ShipDynamicsConfig.DEFAULT.getMaxYawRate(), ship.getYawRate(), 1e-6);
    }

    /** Regression on the lerp clamp: an unclamped factor turns a long tick into a ringing oscillation. */
    @Test
    @DisplayName("a very long step settles the turn rate instead of ringing")
    void hugeStepDoesNotOscillate() {
        final ShipDynamics ship = ship();

        double previousSign = 0;
        int signChanges = 0;
        for (int step = 0; step < 20; step++) {
            ship.advance(5.0, 1);
            assertTrue(ship.getYawRate() <= ShipDynamicsConfig.DEFAULT.getMaxYawRate() + 1e-9,
                    "turn rate overshot on a long step: " + ship.getYawRate());
            final double sign = Math.signum(ship.getYawRate() - ShipDynamicsConfig.DEFAULT.getMaxYawRate() * ship.getRudder());
            if (previousSign != 0 && sign != 0 && sign != previousSign) {
                signChanges++;
            }
            previousSign = sign;
        }

        assertEquals(0, signChanges, "the turn rate crossed its target back and forth");
    }

    @Test
    @DisplayName("heading stays inside [0, 360) turning either way for a long time")
    void headingStaysNormalised() {
        final ShipDynamics starboard = new ShipDynamics(ShipDynamicsConfig.DEFAULT, 350);
        final ShipDynamics port = new ShipDynamics(ShipDynamicsConfig.DEFAULT, 5);

        for (int step = 0; step < 4000; step++) {
            starboard.advance(0.05, 1);
            port.advance(0.05, -1);

            assertTrue(starboard.getHeading() >= 0 && starboard.getHeading() < 360, "heading escaped: " + starboard.getHeading());
            assertTrue(port.getHeading() >= 0 && port.getHeading() < 360, "heading escaped: " + port.getHeading());
        }
    }

    @Test
    @DisplayName("a heading given outside the range is folded in on construction")
    void constructorNormalisesHeading() {
        assertEquals(350.0, new ShipDynamics(ShipDynamicsConfig.DEFAULT, -10).getHeading(), 1e-9);
        assertEquals(10.0, new ShipDynamics(ShipDynamicsConfig.DEFAULT, 730).getHeading(), 1e-9);
    }

    @Test
    @DisplayName("turning costs way, and full rudder costs the whole turn drag")
    void speedFallsWithRudder() {
        final ShipDynamics straight = ship();
        straight.advance(0.05, 0);

        final ShipDynamics turning = settled(1);

        assertEquals(ShipDynamicsConfig.DEFAULT.getBaseSpeed(), straight.getSpeed(), 1e-9);
        assertEquals(ShipDynamicsConfig.DEFAULT.getBaseSpeed() * (1 - ShipDynamicsConfig.DEFAULT.getTurnDrag()),
                turning.getSpeed(), 1e-9);
        assertTrue(turning.getSpeed() < straight.getSpeed());
    }

    @Test
    @DisplayName("wind scales speed linearly")
    void windScalesSpeed() {
        final ShipDynamics becalmed = ship();
        becalmed.setWindFactor(0.5);
        becalmed.advance(0.05, 0);

        final ShipDynamics driven = ship();
        driven.setWindFactor(1.5);
        driven.advance(0.05, 0);

        assertEquals(ShipDynamicsConfig.DEFAULT.getBaseSpeed() * 0.5, becalmed.getSpeed(), 1e-9);
        assertEquals(becalmed.getSpeed() * 3, driven.getSpeed(), 1e-9);
    }

    /**
     * The whole feature hangs off this convention: heading 0 faces {@code +Z}, and the starboard side of a ship facing
     * {@code +Z} is {@code -X}.
     */
    @ParameterizedTest
    @CsvSource({
            "0,     0,  1,  -1,  0",
            "90,   -1,  0,   0, -1",
            "180,   0, -1,   1,  0",
            "270,   1,  0,   0,  1",
    })
    @DisplayName("the forward and starboard unit vectors at the cardinal headings")
    void unitVectors(double heading, double forwardX, double forwardZ, double rightX, double rightZ) {
        final ShipDynamics ship = new ShipDynamics(ShipDynamicsConfig.DEFAULT, heading);

        assertEquals(forwardX, ship.forwardX(), 1e-9);
        assertEquals(forwardZ, ship.forwardZ(), 1e-9);
        assertEquals(rightX, ship.rightX(), 1e-9);
        assertEquals(rightZ, ship.rightZ(), 1e-9);
    }

    @Test
    @DisplayName("a positive rudder turns the ship to starboard")
    void starboardRudderTurnsStarboard() {
        final ShipDynamics ship = new ShipDynamics(ShipDynamicsConfig.DEFAULT, 0);
        for (int step = 0; step < 20; step++) {
            ship.advance(0.05, 1);
        }

        assertTrue(ship.getHeading() > 0 && ship.getHeading() < 90, "heading: " + ship.getHeading());
        assertTrue(ship.forwardX() < 0, "a starboard turn from +Z should swing the bow toward -X");
    }
}
