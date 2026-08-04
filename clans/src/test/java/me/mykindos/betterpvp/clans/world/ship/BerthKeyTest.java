package me.mykindos.betterpvp.clans.world.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BerthKeyTest {

    @Test
    @DisplayName("a key round-trips back to the world and berth it was built from")
    void roundTrip() {
        final String key = BerthKey.of("Season-2/Spawn", "north_berth");

        assertEquals("Season-2/Spawn", BerthKey.worldOf(key));
        assertEquals("north_berth", BerthKey.berthOf(key));
    }

    /**
     * The trap: instanced island worlds are named {@code islands/<template>/<instance>}, so a key holds several
     * slashes. Splitting from the left would put the berth in a world called "islands".
     */
    @ParameterizedTest
    @CsvSource({
            "Season-2/Spawn,             north_berth",
            "islands/mining/a1b2c3d4,    berth",
            "world,                      quay",
            "islands/fishing/0f0f0f0f,   south_berth",
    })
    @DisplayName("world names containing slashes still split correctly")
    void slashesInWorldNames(String worldName, String berthId) {
        final String key = BerthKey.of(worldName, berthId);

        assertEquals(worldName, BerthKey.worldOf(key));
        assertEquals(berthId, BerthKey.berthOf(key));
    }

    @Test
    @DisplayName("two berths of the same name in different worlds are different moorings")
    void sameBerthIdInTwoWorldsIsDistinct() {
        assertNotEquals(
                BerthKey.of("islands/mining/aaaaaaaa", "berth"),
                BerthKey.of("islands/mining/bbbbbbbb", "berth"));
    }

    @Test
    @DisplayName("a malformed key degrades rather than throwing")
    void malformedKeyIsSafe() {
        assertEquals("", BerthKey.worldOf("no-separator"));
        assertEquals("no-separator", BerthKey.berthOf("no-separator"));
    }
}
