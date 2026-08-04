package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.core.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped distances only work in one order. Getting them out of it is invisible in play — the sea simply never has
 * anything on it, or lands the crew the instant it does — so the ordering is asserted rather than trusted.
 */
class SightingThresholdsTest {

    private static double configured(String field) throws NoSuchFieldException {
        final Config annotation = SightingConfig.class.getDeclaredField(field).getAnnotation(Config.class);
        assertNotNull(annotation, field + " is no longer configurable");
        return Double.parseDouble(annotation.defaultValue());
    }

    @Test
    @DisplayName("arrival, the scale ramp, the spawn band and the despawn distance are in the only order that works")
    void thresholdsAreOrdered() throws NoSuchFieldException {
        final double arrival = configured("arrivalDistance");
        final double rampStart = configured("rampStartDistance");
        final double spawnMin = configured("spawnMinDistance");
        final double spawnMax = configured("spawnMaxDistance");
        final double despawn = configured("despawnDistance");

        assertTrue(arrival < rampStart, "the icon would already be full size when the crew arrived");
        assertTrue(rampStart < spawnMin, "an island would spawn already growing");
        assertTrue(spawnMin < spawnMax, "the spawn band is inside out");
        assertTrue(spawnMax < despawn, "an island would be culled on the tick it appeared");
    }

    @Test
    @DisplayName("a sighting is drawn no further out than the crew can be sent it")
    void thePinIsWithinRenderDistance() throws NoSuchFieldException {
        assertTrue(configured("pinDistance") <= configured("rampStartDistance"),
                "the marker would be pinned beyond the range it starts growing at");
    }
}
