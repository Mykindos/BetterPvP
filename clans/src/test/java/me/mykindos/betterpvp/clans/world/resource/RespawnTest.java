package me.mykindos.betterpvp.clans.world.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespawnTest {

    private static NavigableMap<Integer, Double> thresholds() {
        final NavigableMap<Integer, Double> map = new TreeMap<>();
        map.put(0, 1.0);
        map.put(5, 1.5);
        map.put(10, 2.0);
        return map;
    }

    @Test
    @DisplayName("speedBuff picks the highest threshold not exceeding the player count")
    void speedBuffThresholds() {
        final NavigableMap<Integer, Double> t = thresholds();
        assertEquals(1.0, Respawn.speedBuff(1.0, 0, t));
        assertEquals(1.0, Respawn.speedBuff(1.0, 4, t));
        assertEquals(1.5, Respawn.speedBuff(1.0, 5, t));
        assertEquals(1.5, Respawn.speedBuff(1.0, 9, t));
        assertEquals(2.0, Respawn.speedBuff(1.0, 10, t));
        assertEquals(2.0, Respawn.speedBuff(1.0, 250, t));
    }

    @Test
    @DisplayName("speedBuff applies the bonus multiplier and defaults to 1.0 below the lowest threshold")
    void speedBuffBonusAndDefault() {
        final NavigableMap<Integer, Double> t = thresholds();
        assertEquals(3.0, Respawn.speedBuff(2.0, 5, t)); // 2.0 * 1.5

        final NavigableMap<Integer, Double> noZero = new TreeMap<>();
        noZero.put(5, 1.5);
        assertEquals(2.0, Respawn.speedBuff(2.0, 0, noZero)); // no match -> base 1.0 * bonus 2.0
    }

    @Test
    @DisplayName("isReady fires once the scaled delay has elapsed")
    void isReady() {
        assertFalse(Respawn.isReady(0L, 60.0, 1.0, 59_999L));
        assertTrue(Respawn.isReady(0L, 60.0, 1.0, 60_000L));
        // double speed halves the required wait
        assertTrue(Respawn.isReady(0L, 60.0, 2.0, 30_000L));
        assertFalse(Respawn.isReady(0L, 60.0, 2.0, 29_999L));
    }

    @Test
    @DisplayName("isReady treats a non-positive modifier as 1.0")
    void isReadyGuardsModifier() {
        assertTrue(Respawn.isReady(0L, 60.0, 0.0, 60_000L));
        assertTrue(Respawn.isReady(0L, 60.0, -3.0, 60_000L));
    }
}
