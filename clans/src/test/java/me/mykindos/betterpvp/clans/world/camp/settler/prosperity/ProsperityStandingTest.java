package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProsperityStandingTest {

    @Test
    void ac9_theChangeIsProsperityMinusTheSnapshot() {
        assertEquals(50, new ProsperityStanding(300, 250).change());
        assertEquals(-70, new ProsperityStanding(130, 200).change());
        assertEquals(0, new ProsperityStanding(90, 90).change());
    }
}
