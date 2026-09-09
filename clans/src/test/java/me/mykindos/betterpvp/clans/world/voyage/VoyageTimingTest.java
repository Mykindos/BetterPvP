package me.mykindos.betterpvp.clans.world.voyage;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoyageTimingTest {

    private static final VoyageTiming TIMING = new VoyageTiming(30, 90, 0.25);

    /** Always lands, so only the floor and ceiling decide the outcome. */
    private static final double ALWAYS = 0.0;

    /** Never lands on odds alone. */
    private static final double NEVER = 0.99;

    @ParameterizedTest
    @ValueSource(longs = {0, 10, 29})
    @DisplayName("nothing happens before the floor, however the dice fall")
    void neverArrivesBeforeMinimum(long elapsed) {
        assertFalse(TIMING.arrives(elapsed, ALWAYS),
                "a crossing that can end at once is not a crossing");
    }

    @Test
    @DisplayName("a lucky roll lands as soon as the floor is reached")
    void arrivesOnFirstRollWhenLucky() {
        assertTrue(TIMING.arrives(30, ALWAYS));
    }

    @Test
    @DisplayName("an unlucky roll keeps them at sea")
    void staysAtSeaWhenUnlucky() {
        assertFalse(TIMING.arrives(30, NEVER));
        assertFalse(TIMING.arrives(60, NEVER));
    }

    /**
     * The reason the ceiling exists. With a flat per-roll chance and no bound, a small share of crossings run far past
     * the intended length, and those are the ones that get reported as broken.
     */
    @ParameterizedTest
    @ValueSource(longs = {90, 120, 6000})
    @DisplayName("the ceiling lands them however badly the dice have gone")
    void alwaysArrivesAtMaximum(long elapsed) {
        assertTrue(TIMING.arrives(elapsed, NEVER));
    }

    @Test
    @DisplayName("the sample is compared strictly against the chance")
    void chanceBoundaryIsExclusive() {
        final VoyageTiming quarter = new VoyageTiming(0, 1000, 0.25);
        assertTrue(quarter.arrives(10, 0.249));
        assertFalse(quarter.arrives(10, 0.25), "a sample equal to the chance must not count as a hit");
        assertFalse(quarter.arrives(10, 0.9));
    }

    @Test
    @DisplayName("zero chance means every crossing runs to the ceiling")
    void zeroChanceRunsToCeiling() {
        final VoyageTiming never = new VoyageTiming(10, 40, 0.0);
        assertFalse(never.arrives(20, 0.0));
        assertTrue(never.arrives(40, 0.0));
    }

    @Test
    @DisplayName("certain odds land on the first roll after the floor")
    void certainChanceLandsAtFloor() {
        final VoyageTiming certain = new VoyageTiming(10, 400, 1.0);
        assertFalse(certain.arrives(9, 0.999));
        assertTrue(certain.arrives(10, 0.999));
    }

    @Test
    @DisplayName("isRolling marks the window where the crossing is genuinely in doubt")
    void isRollingWindow() {
        assertFalse(TIMING.isRolling(29));
        assertTrue(TIMING.isRolling(30));
        assertTrue(TIMING.isRolling(89));
        assertFalse(TIMING.isRolling(90));
    }

    /**
     * Misconfiguration should degrade to something playable rather than to a crossing that can never end or one that
     * ends instantly.
     */
    @Test
    @DisplayName("a ceiling below the floor becomes a fixed-length crossing")
    void invertedBoundsCollapseToFixedLength() {
        final VoyageTiming inverted = new VoyageTiming(60, 10, 0.5);

        assertEquals(60, inverted.getMinSeconds());
        assertEquals(60, inverted.getMaxSeconds());
        assertFalse(inverted.arrives(59, ALWAYS));
        assertTrue(inverted.arrives(60, NEVER));
    }

    @Test
    @DisplayName("out-of-range configuration is clamped rather than trusted")
    void configurationIsClamped() {
        assertEquals(0, new VoyageTiming(-30, 60, 0.5).getMinSeconds());
        assertEquals(1.0, new VoyageTiming(0, 60, 4.0).getChancePerRoll());
        assertEquals(0.0, new VoyageTiming(0, 60, -1.0).getChancePerRoll());
    }

    @Test
    @DisplayName("the default crossing has a floor, a ceiling and odds between them")
    void defaultIsSane() {
        assertTrue(VoyageTiming.DEFAULT.getMinSeconds() > 0);
        assertTrue(VoyageTiming.DEFAULT.getMaxSeconds() > VoyageTiming.DEFAULT.getMinSeconds());
        assertTrue(VoyageTiming.DEFAULT.getChancePerRoll() > 0
                && VoyageTiming.DEFAULT.getChancePerRoll() < 1);
    }
}
