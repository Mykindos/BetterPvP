package me.mykindos.betterpvp.core.world.settler.recruit;

import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerOddsTest {

    @Test
    void weightsDecideHowOftenEachComesUp() {
        final Map<SettlerRarity, Double> rarities = new EnumMap<>(SettlerRarity.class);
        rarities.put(SettlerRarity.COMMON, 3.0);
        rarities.put(SettlerRarity.LEGENDARY, 1.0);
        final SettlerOdds odds = new SettlerOdds(rarities, Map.of());
        final Random random = new Random(1);

        int legendary = 0;
        for (int i = 0; i < 10_000; i++) {
            if (odds.rarity(random) == SettlerRarity.LEGENDARY) {
                legendary++;
            }
        }
        assertTrue(legendary > 2_200 && legendary < 2_800, "about a quarter, got " + legendary);
    }

    @Test
    void noneMeansNoProfessionAndNothingToPickFallsBackSafely() {
        final Map<String, Double> professions = new LinkedHashMap<>();
        professions.put(SettlerOdds.NONE, 1.0);
        final SettlerOdds odds = new SettlerOdds(Map.of(), professions);

        assertNull(odds.profession(new Random(1)));
        assertEquals(SettlerRarity.COMMON, odds.rarity(new Random(1)), "no rarity weights rolls common");
        assertNull(SettlerOdds.pick(Map.of("builder", 0.0), new Random(1)));
    }
}
