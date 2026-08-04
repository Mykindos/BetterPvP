package me.mykindos.betterpvp.clans.world.discovery.hazard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteeringOverrideTest {

    @Test
    @DisplayName("with nothing holding the wheel the crew's own input is passed straight through")
    void idlePassesCrewInputThrough() {
        final SteeringOverride override = new SteeringOverride();

        assertFalse(override.isActive(0));
        assertEquals(-1, override.apply(-1, 0));
        assertEquals(0, override.apply(0, 0));
        assertEquals(1, override.apply(1, 0));
    }

    @Test
    @DisplayName("a seized wheel outranks whatever the crew is doing with the controls")
    void seizureBeatsCrewInput() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(1, 0, 2000);

        assertEquals(1, override.apply(-1, 0), "steering hard the other way does not help");
        assertEquals(1, override.apply(0, 1000), "nor does letting go");
    }

    @Test
    @DisplayName("the side is taken as a sign, so any negative is to port")
    void sideIsASign() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(-5, 0, 1000);

        assertEquals(-1, override.getSide());
        assertEquals(-1, override.apply(1, 500));
    }

    @Test
    @DisplayName("the grip holds for the whole duration and counts down while it does")
    void gripHoldsForItsDuration() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(1, 1_000, 2000);

        assertTrue(override.isActive(1_000));
        assertEquals(2000, override.remaining(1_000));

        assertTrue(override.isActive(2_500), "half way through it is still holding");
        assertEquals(1500, override.remaining(1_500));

        assertTrue(override.isActive(2_999));
        assertFalse(override.isActive(3_000), "the wheel is let go exactly when the duration is up");
        assertEquals(0, override.remaining(3_000));
    }

    @Test
    @DisplayName("control goes back to the crew on the tick the grip lapses")
    void expiryHandsControlBack() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(1, 0, 2000);

        assertEquals(1, override.apply(-1, 1_999));
        assertEquals(-1, override.apply(-1, 2_000));
        assertEquals(0, override.getSide(), "and the lapsed grip clears itself rather than lingering");
    }

    @Test
    @DisplayName("a second seizure replaces the first rather than stacking with it")
    void reseizingReplaces() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(1, 0, 2000);
        override.seize(-1, 1_000, 2000);

        assertEquals(-1, override.apply(0, 1_500));
        assertEquals(3_000, override.getUntil());
    }

    @Test
    @DisplayName("releasing hands the wheel back immediately")
    void releaseHandsBackAtOnce() {
        final SteeringOverride override = new SteeringOverride();
        override.seize(1, 0, 10_000);
        override.release();

        assertFalse(override.isActive(0));
        assertEquals(-1, override.apply(-1, 0));
    }
}
