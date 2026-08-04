package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardSpawnPolicyTest {

    private static final HazardSpawnPolicy POLICY = new HazardSpawnPolicy(0.5, 100, 25, 75, 0.3, 0.15);

    /** Draws the scripted values in order, so a whole spawn decision can be dictated rather than sampled. */
    private static DoubleSupplier scripted(double... draws) {
        final double[] values = draws.clone();
        return new DoubleSupplier() {
            private int index;

            @Override
            public double getAsDouble() {
                return values[index++];
            }
        };
    }

    @Test
    @DisplayName("a high first draw means no hazard, and no further draws are spent")
    void highFirstDrawSkips() {
        assertTrue(POLICY.next(0, 1.0, scripted(0.9)).isEmpty());
    }

    @Test
    @DisplayName("a low first draw spawns")
    void lowFirstDrawSpawns() {
        assertTrue(POLICY.next(0, 1.0, scripted(0.1, 0.9, 0.5)).isPresent());
    }

    @Test
    @DisplayName("the first draw is measured against rate times the step, not the rate alone")
    void rateScalesWithTheStep() {
        assertTrue(POLICY.next(0, 0.1, scripted(0.2)).isEmpty(), "0.2 is above 0.5 * 0.1");
        assertTrue(POLICY.next(0, 1.0, scripted(0.2, 0.9, 0.5)).isPresent(), "0.2 is below 0.5 * 1.0");
    }

    @Test
    @DisplayName("a second draw under the flank chance puts the hazard abeam")
    void flankSpawnsAbeam() {
        final HazardSpawn port = POLICY.next(0, 1.0, scripted(0.1, 0.2, 0.4)).orElseThrow();
        final HazardSpawn starboard = POLICY.next(0, 1.0, scripted(0.1, 0.2, 0.6)).orElseThrow();

        assertEquals(-90, port.getBearing(), 1e-9);
        assertEquals(90, starboard.getBearing(), 1e-9);
        assertEquals(75, port.getDistance(), 1e-9);
        assertEquals(75, starboard.getDistance(), 1e-9);
    }

    @Test
    @DisplayName("a second draw over the flank chance puts the hazard in the cone ahead")
    void aheadSpawnsInTheCone() {
        final HazardSpawn hardPort = POLICY.next(0, 1.0, scripted(0.1, 0.8, 0.0)).orElseThrow();
        final HazardSpawn centred = POLICY.next(0, 1.0, scripted(0.1, 0.8, 0.5)).orElseThrow();
        final HazardSpawn hardStarboard = POLICY.next(0, 1.0, scripted(0.1, 0.8, 1.0)).orElseThrow();

        assertEquals(-25, hardPort.getBearing(), 1e-9);
        assertEquals(0, centred.getBearing(), 1e-9);
        assertEquals(25, hardStarboard.getBearing(), 1e-9);
        assertEquals(100, centred.getDistance(), 1e-9);
    }

    @Test
    @DisplayName("an ahead spawn never leaves the cone, however the side draw falls")
    void aheadStaysInsideTheSpread() {
        for (int step = 0; step <= 100; step++) {
            final HazardSpawn spawn = POLICY.next(0, 1.0, scripted(0.1, 0.8, step / 100.0)).orElseThrow();
            assertTrue(Math.abs(spawn.getBearing()) <= 25 + 1e-9, "bearing left the cone: " + spawn.getBearing());
        }
    }

    @Test
    @DisplayName("depth raises the rate, so a draw that missed at depth zero lands deeper")
    void depthRaisesTheRate() {
        assertTrue(POLICY.next(0, 1.0, scripted(0.6)).isEmpty());
        assertTrue(POLICY.next(4, 1.0, scripted(0.6, 0.9, 0.5)).isPresent(), "0.6 is below 0.5 * 1.6");
    }

    @Test
    @DisplayName("depth is inert when it is zero, which is what callers pass today")
    void depthZeroChangesNothing() {
        final Optional<HazardSpawn> withScale = POLICY.next(0, 1.0, scripted(0.49, 0.8, 0.5));
        final Optional<HazardSpawn> withoutScale =
                new HazardSpawnPolicy(0.5, 100, 25, 75, 0.3, 0.0).next(0, 1.0, scripted(0.49, 0.8, 0.5));

        assertEquals(withoutScale, withScale);
    }

    @Test
    @DisplayName("the default policy spawns at the design's ranges, at a rate somebody could sail through")
    void defaultsMatchTheDesign() {
        assertEquals(100, HazardSpawnPolicy.DEFAULT.getAheadDistance(), 1e-9);
        assertEquals(75, HazardSpawnPolicy.DEFAULT.getFlankDistance(), 1e-9);

        // Deliberately a wide band. The rate is tuned by feel and moves often; this only catches an order-of-magnitude
        // slip, where the sea is either empty for minutes or unsailable.
        final double meanSecondsBetweenSpawns = 1.0 / HazardSpawnPolicy.DEFAULT.getSpawnsPerSecond();
        assertTrue(meanSecondsBetweenSpawns >= 1 && meanSecondsBetweenSpawns <= 60,
                "hazards would arrive every " + meanSecondsBetweenSpawns + "s");
    }

    @Test
    @DisplayName("over a long run the default policy uses both flanks and the cone ahead")
    void defaultsProduceBothShapesOfSpawn() {
        final Random random = new Random(20260802L);

        boolean sawPortFlank = false;
        boolean sawStarboardFlank = false;
        boolean sawAhead = false;
        for (int step = 0; step < 5000; step++) {
            final Optional<HazardSpawn> spawn = HazardSpawnPolicy.DEFAULT.next(0, 1.0, random::nextDouble);
            if (spawn.isEmpty()) {
                continue;
            }
            final double bearing = spawn.get().getBearing();
            sawPortFlank |= bearing == -90;
            sawStarboardFlank |= bearing == 90;
            sawAhead |= Math.abs(bearing) <= 25;
        }

        assertTrue(sawPortFlank, "no hazard ever came up the port side");
        assertTrue(sawStarboardFlank, "no hazard ever came up the starboard side");
        assertTrue(sawAhead, "no hazard was ever placed ahead");
        assertFalse(HazardSpawnPolicy.DEFAULT.next(0, 1.0, scripted(0.99)).isPresent());
    }
}
