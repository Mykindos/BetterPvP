package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestedLandingPolicyTest {

    private static final long WINDOW = 60_000L;
    private static final UUID ISLAND = UUID.randomUUID();

    private static ContestedLandingPolicy withOneLanding(int crewSize, long at) {
        final ContestedLandingPolicy policy = new ContestedLandingPolicy(WINDOW);
        policy.record("mining", crewSize, ISLAND, "east-quay", at);
        return policy;
    }

    @Test
    @DisplayName("a crew of the same size sailing in behind them is sent to the same island")
    void equalCrewsContest() {
        final ContestedLandingPolicy policy = withOneLanding(4, 0);

        final ContestedLanding contested = policy.contestFor("mining", 4, 1_000).orElse(null);
        assertNotNull(contested);
        assertEquals(ISLAND, contested.getInstanceId());
        assertEquals("east-quay", contested.getArrivalPointName(),
                "the shore they used is what a contesting crew is put down away from");
    }

    @Test
    @DisplayName("one more or one fewer is still a fair fight")
    void crewsWithinOneContest() {
        assertTrue(withOneLanding(4, 0).contestFor("mining", 5, 1_000).isPresent());
        assertTrue(withOneLanding(4, 0).contestFor("mining", 3, 1_000).isPresent());
    }

    @Test
    @DisplayName("two more is not, and they get an island of their own")
    void mismatchedCrewsDoNotContest() {
        assertTrue(withOneLanding(4, 0).contestFor("mining", 6, 1_000).isEmpty());
        assertTrue(withOneLanding(4, 0).contestFor("mining", 2, 1_000).isEmpty());
    }

    @Test
    @DisplayName("a crew steering for somewhere else is not contesting anything")
    void otherTemplatesDoNotContest() {
        assertTrue(withOneLanding(4, 0).contestFor("fishing", 4, 1_000).isEmpty());
    }

    @Test
    @DisplayName("a landing older than the window is over, and the island is theirs")
    void staleLandingsDoNotContest() {
        assertTrue(withOneLanding(4, 0).contestFor("mining", 4, WINDOW + 1).isEmpty());
    }

    @Test
    @DisplayName("stale landings are dropped rather than kept, so the list does not grow all season")
    void staleLandingsAreForgotten() {
        final ContestedLandingPolicy policy = withOneLanding(4, 0);
        assertEquals(1, policy.size());

        policy.record("fishing", 2, UUID.randomUUID(), "", WINDOW + 1);
        assertEquals(1, policy.size(), "recording is what expires the ones that have gone cold");
    }

    @Test
    @DisplayName("a landing exactly on the window is still contestable")
    void theWindowIsInclusive() {
        assertFalse(withOneLanding(4, 0).contestFor("mining", 4, WINDOW).isEmpty());
    }
}
