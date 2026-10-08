package me.mykindos.betterpvp.core.world.settler.recruit;

import me.mykindos.betterpvp.core.world.settler.Settler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerCandidateTest {

    @Test
    void ac3_aCandidateAsksAPriceAndWaitsUntilItsTimeIsUp() {
        final Settler settler = new Settler();
        final SettlerCandidate candidate = new SettlerCandidate(settler, 4_500, 1_000);

        assertSame(settler, candidate.getSettler());
        assertEquals(4_500, candidate.getPrice());
        assertFalse(candidate.isExpired(999));
        assertTrue(candidate.isExpired(1_000), "expired from the moment itself");
        assertTrue(candidate.isExpired(5_000));
    }

    @Test
    void ac3_zeroMeansFreeAndWaitingForever() {
        final SettlerCandidate candidate = new SettlerCandidate(new Settler(), 0, 0);

        assertEquals(0, candidate.getPrice());
        assertFalse(candidate.isExpired(0));
        assertFalse(candidate.isExpired(Long.MAX_VALUE));
    }
}
