package me.mykindos.betterpvp.core.world.site;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrivalDistributionTest {

    private static final List<String> DOCKS = List.of("North Harbour", "Saltmere Quay", "Old Pier");

    @Test
    @DisplayName("random stays within the candidate list")
    void randomIsInRange() {
        final ArrivalDistribution random = ArrivalDistribution.random();
        for (int attempt = 0; attempt < 200; attempt++) {
            final int index = random.select(DOCKS);
            assertTrue(index >= 0 && index < DOCKS.size(), "index out of range: " + index);
        }
    }

    @Test
    @DisplayName("random eventually uses every dock, so no landing is dead map")
    void randomReachesEveryDock() {
        final ArrivalDistribution random = ArrivalDistribution.random();
        final Set<Integer> seen = new HashSet<>();
        for (int attempt = 0; attempt < 300; attempt++) {
            seen.add(random.select(DOCKS));
        }
        assertEquals(DOCKS.size(), seen.size());
    }

    @Test
    @DisplayName("round-robin visits each dock in turn and wraps")
    void roundRobinCycles() {
        final ArrivalDistribution rotating = ArrivalDistribution.roundRobin();

        assertEquals(0, rotating.select(DOCKS));
        assertEquals(1, rotating.select(DOCKS));
        assertEquals(2, rotating.select(DOCKS));
        assertEquals(0, rotating.select(DOCKS), "the rotation wraps rather than running off the end");
    }

    @Test
    @DisplayName("two round-robin distributions count independently")
    void roundRobinStateIsPerInstance() {
        final ArrivalDistribution first = ArrivalDistribution.roundRobin();
        final ArrivalDistribution second = ArrivalDistribution.roundRobin();

        first.select(DOCKS);
        first.select(DOCKS);

        assertEquals(0, second.select(DOCKS), "one destination's rotation must not drive another's");
    }

    @Test
    @DisplayName("fixed always picks the named dock")
    void fixedPicksItsDock() {
        final ArrivalDistribution quay = ArrivalDistribution.fixed("Saltmere Quay");

        assertEquals(1, quay.select(DOCKS));
        assertEquals(1, quay.select(DOCKS));
    }

    @Test
    @DisplayName("fixed matches a dock name regardless of case")
    void fixedIsCaseInsensitive() {
        assertEquals(2, ArrivalDistribution.fixed("old pier").select(DOCKS));
    }

    /**
     * A renamed marker should mean people land somewhere slightly wrong, not that the destination stops being
     * reachable at all.
     */
    @Test
    @DisplayName("fixed falls back to the first dock when its own is missing")
    void fixedFallsBackWhenDockRenamed() {
        assertEquals(0, ArrivalDistribution.fixed("Vanished Wharf").select(DOCKS));
    }

    @Test
    @DisplayName("an empty candidate list is answered rather than thrown on")
    void emptyCandidatesAreSafe() {
        assertEquals(0, ArrivalDistribution.random().select(List.of()));
        assertEquals(0, ArrivalDistribution.roundRobin().select(List.of()));
        assertEquals(0, ArrivalDistribution.fixed("anything").select(List.of()));
    }

    @Test
    @DisplayName("a single dock is always the answer, whatever the distribution")
    void singleDockAlwaysWins() {
        final List<String> one = List.of("Only Quay");
        assertEquals(0, ArrivalDistribution.random().select(one));
        assertEquals(0, ArrivalDistribution.roundRobin().select(one));
    }

    @Test
    @DisplayName("byName reads the authored distributions and defaults to random")
    void byNameReadsConfig() {
        final ArrivalDistribution rotating = ArrivalDistribution.byName("round-robin");
        assertEquals(0, rotating.select(DOCKS));
        assertEquals(1, rotating.select(DOCKS));

        assertEquals(0, ArrivalDistribution.byName("first").select(DOCKS));

        final int fromNonsense = ArrivalDistribution.byName("not-a-distribution").select(DOCKS);
        assertTrue(fromNonsense >= 0 && fromNonsense < DOCKS.size());
    }
}
