package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RudderGaugeTest {

    @Test
    @DisplayName("hard to port lights the first character and hard to starboard the last")
    void endpointsAreTheEndsOfTheBar() {
        assertEquals(0, RudderGauge.greenIndex(-1.0));
        assertEquals(RudderGauge.WIDTH - 1, RudderGauge.greenIndex(1.0));
    }

    @Test
    @DisplayName("a centred rudder lights the exact middle character")
    void centreIsTheMiddleCharacter() {
        assertEquals((RudderGauge.WIDTH - 1) / 2, RudderGauge.greenIndex(0.0));
    }

    @ParameterizedTest
    @CsvSource({
            "-1.0,  0",
            "-0.75, 2",
            "-0.5,  4",
            "-0.25, 6",
            "0.0,   8",
            "0.25, 10",
            "0.5,  12",
            "0.75, 14",
            "1.0,  16"
    })
    @DisplayName("the mark moves evenly across the bar as the wheel comes over")
    void marksAreEvenlySpaced(double rudder, int expected) {
        assertEquals(expected, RudderGauge.greenIndex(rudder));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-40.0, -1.5, -1.0000001, 1.0000001, 1.5, 40.0})
    @DisplayName("a reading past the stops still lands on the bar rather than off the end of it")
    void outOfRangeReadingsAreClamped(double rudder) {
        final int index = RudderGauge.greenIndex(rudder);

        assertTrue(index >= 0 && index < RudderGauge.WIDTH, "the mark fell off the gauge: " + index);
    }

    @Test
    @DisplayName("the mark never sticks: neighbouring wheel positions read as neighbouring characters")
    void indexIsMonotonic() {
        int previous = RudderGauge.greenIndex(-1.0);
        for (int step = 1; step <= 200; step++) {
            final int index = RudderGauge.greenIndex(-1.0 + step / 100.0);
            assertTrue(index >= previous && index - previous <= 1, "the mark jumped to " + index);
            previous = index;
        }
        assertEquals(RudderGauge.WIDTH - 1, previous);
    }
}
