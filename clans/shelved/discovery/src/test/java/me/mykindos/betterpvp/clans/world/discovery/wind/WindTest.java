package me.mykindos.betterpvp.clans.world.discovery.wind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindTest {

    @Test
    @DisplayName("every factor lands in a band, and the bands run in order")
    void everyFactorHasABand() {
        WindBand previous = WindBand.of(0.0);
        for (double factor = 0.0; factor <= 3.0; factor += 0.01) {
            final WindBand band = WindBand.of(factor);
            assertTrue(band.ordinal() >= previous.ordinal(), "the wind read weaker as it strengthened at " + factor);
            previous = band;
        }
    }

    @Test
    @DisplayName("the extremes read as the extremes")
    void theEndsOfTheRangeAreNamed() {
        assertEquals(WindBand.BECALMED, WindBand.of(0.0));
        assertEquals(WindBand.BECALMED, WindBand.of(-1.0));
        assertEquals(WindBand.FAIR, WindBand.of(1.0));
        assertEquals(WindBand.GALE, WindBand.of(Double.MAX_VALUE));
        assertEquals(WindBand.GALE, WindBand.of(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("the shipped wind range can reach every band")
    void theShippedRangeUsesEveryBand() {
        final Set<WindBand> reached = EnumSet.noneOf(WindBand.class);
        for (double factor = 0.55; factor <= 1.50; factor += 0.01) {
            reached.add(WindBand.of(factor));
        }
        assertEquals(EnumSet.allOf(WindBand.class), reached);
    }

    @Test
    @DisplayName("every band has somewhere to draw a phrasing from, and they are all distinct keys")
    void everyBandHasAPool() {
        for (WindBand band : WindBand.values()) {
            assertTrue(band.variants() > 0, band + " has nothing to say");

            final Set<String> keys = new HashSet<>();
            for (int variant = 0; variant < band.variants(); variant++) {
                assertTrue(keys.add(band.messageKey(variant)), band + " repeats a key at " + variant);
            }
        }
    }

    @Test
    @DisplayName("a variant outside the pool is folded back into it rather than naming a key nobody wrote")
    void variantsAreFolded() {
        assertEquals(WindBand.FAIR.messageKey(0), WindBand.FAIR.messageKey(WindBand.FAIR.variants()));
        assertEquals(WindBand.FAIR.messageKey(0), WindBand.FAIR.messageKey(-WindBand.FAIR.variants()));
    }

    @Test
    @DisplayName("the wind eases toward its target rather than snapping onto it")
    void theWindEases() {
        final Wind wind = new Wind();
        wind.shift(1.5, 0, 10_000);

        wind.advance(1.0, 0.25);
        assertTrue(wind.getFactor() > 1.0 && wind.getFactor() < 1.5,
                "the ship changed speed instantly: " + wind.getFactor());

        for (int step = 0; step < 200; step++) {
            wind.advance(1.0, 0.25);
        }
        assertEquals(1.5, wind.getFactor(), 1e-6, "the wind never got where it was going");
    }

    @Test
    @DisplayName("a step within the same band says nothing")
    void unchangedBandsAreSilent() {
        final Wind wind = new Wind();
        wind.shift(1.05, 0, 10_000);

        for (int step = 0; step < 50; step++) {
            assertTrue(wind.advance(1.0, 0.25).isEmpty(), "the crew were told about weather that had not changed");
        }
        assertEquals(WindBand.FAIR, wind.getBand());
    }

    @Test
    @DisplayName("crossing into a new band is reported exactly once")
    void aBandChangeIsReportedOnce() {
        final Wind wind = new Wind();
        wind.shift(0.5, 0, 10_000);

        WindBand announced = null;
        int announcements = 0;
        for (int step = 0; step < 400; step++) {
            final Optional<WindBand> change = wind.advance(1.0, 0.25);
            if (change.isPresent()) {
                announcements++;
                assertNotEquals(announced, change.get());
                announced = change.get();
            }
        }

        assertEquals(WindBand.BECALMED, wind.getBand());
        assertEquals(2, announcements, "the wind passed through SLACK on its way down, and no further");
    }

    @Test
    @DisplayName("a fresh wind is due immediately and not again until its period is up")
    void theScheduleHoldsForAPeriod() {
        final Wind wind = new Wind();
        assertTrue(wind.due(0), "the first tick has to set the weather");

        wind.shift(1.2, 1_000, 45_000);
        assertFalse(wind.due(45_999));
        assertTrue(wind.due(46_000));
    }
}
