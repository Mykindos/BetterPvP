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
    void ac1_rarityComesUpInProportionToItsWeight() {
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
    void ac1_negativeWeightsCountAsZeroAndNoWeightRollsCommon() {
        final Map<SettlerRarity, Double> rarities = new EnumMap<>(SettlerRarity.class);
        rarities.put(SettlerRarity.LEGENDARY, -5.0);
        rarities.put(SettlerRarity.RARE, 1.0);
        final SettlerOdds odds = new SettlerOdds(rarities, Map.of());
        final Random random = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            assertEquals(SettlerRarity.RARE, odds.rarity(random));
        }

        assertEquals(SettlerRarity.COMMON, new SettlerOdds(Map.of(), Map.of()).rarity(new Random(1)));
        assertEquals(SettlerRarity.COMMON, new SettlerOdds(Map.of(SettlerRarity.RARE, 0.0), Map.of())
                .rarity(new Random(1)));
    }

    @Test
    void ac2_professionComesUpByWeightAndNoneMeansNoProfession() {
        final Map<String, Double> professions = new LinkedHashMap<>();
        professions.put("builder", 1.0);
        professions.put(SettlerOdds.NONE, 1.0);
        final SettlerOdds odds = new SettlerOdds(Map.of(), professions);
        final Random random = new Random(1);

        int builders = 0;
        int none = 0;
        for (int i = 0; i < 10_000; i++) {
            final String rolled = odds.profession(random);
            if (rolled == null) {
                none++;
            } else {
                assertEquals("builder", rolled);
                builders++;
            }
        }
        assertTrue(builders > 4_500 && none > 4_500, "about half each, got " + builders + " and " + none);
    }

    @Test
    void ac2_nothingToPickGivesNoProfession() {
        assertNull(new SettlerOdds(Map.of(), Map.of()).profession(new Random(1)));
        assertNull(new SettlerOdds(Map.of(), Map.of("builder", 0.0)).profession(new Random(1)));
        assertNull(new SettlerOdds(Map.of(), Map.of("builder", -1.0)).profession(new Random(1)));
        assertNull(SettlerOdds.pick(Map.of("builder", 0.0), new Random(1)));
    }
}
