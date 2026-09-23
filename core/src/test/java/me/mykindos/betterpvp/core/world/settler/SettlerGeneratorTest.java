package me.mykindos.betterpvp.core.world.settler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerGeneratorTest {

    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final TraitRegistry traits = new TraitRegistry();
    private final SettlerGenerator generator = new SettlerGenerator(professions, traits);

    private SettlerTable table(double tradeOffChance) {
        return new SettlerTable(
                Map.of(SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, tradeOffChance),
                        SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, tradeOffChance)),
                List.of("Aldric"), List.of("Tanner"),
                Map.of("dungeon", List.of("history.dungeon")));
    }

    @BeforeEach
    void setUp() {
        professions.register(Profession.construction("builder", "builder", "model", List.of("mason", "smith")));
        professions.register(Profession.workplace("farmer", "farmer", "farm", "model"));
        traits.register(trait("steady").profession("builder").build());
        traits.register(trait("greedy").profession("builder").tradeOff(true).build());
        traits.register(trait("green_thumb").profession("farmer").build());
        traits.register(trait("bard").build());
        traits.register(trait("loyal").build());
        traits.register(trait("beloved").minimumRarity(SettlerRarity.LEGENDARY).build());
    }

    private static Trait.TraitBuilder trait(String id) {
        return Trait.builder().id(id).key(id).group(TraitGroup.PERSONAL);
    }

    private Settler roll(SettlerRarity rarity, String profession, double tradeOffChance, long seed) {
        return generator.roll(SettlerTemplate.builder().rarity(rarity).profession(profession).source("dungeon")
                .historyArg("the Sunken Keep").build(), table(tradeOffChance), new Random(seed));
    }

    @Test
    void rarityDecidesHowManyTraitsAndTheyNeverRepeat() {
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(1, roll(SettlerRarity.COMMON, "builder", 0.3, seed).getTraits().size());
            final List<String> legendary = roll(SettlerRarity.LEGENDARY, "builder", 0.3, seed).getTraits();
            assertEquals(3, legendary.size());
            assertEquals(3, new HashSet<>(legendary).size());
        }
    }

    @Test
    void traitsOnlyRollOnTheProfessionsAndRaritiesTheyAllow() {
        final Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            final Settler farmer = roll(SettlerRarity.COMMON, "farmer", 0, seed);
            assertFalse(farmer.hasTrait("steady"));
            assertFalse(farmer.hasTrait("beloved"));
            seen.addAll(roll(SettlerRarity.LEGENDARY, null, 0, seed).getTraits());
        }
        assertTrue(seen.contains("beloved"));
        assertFalse(seen.contains("steady"));
        assertFalse(seen.contains("green_thumb"));
    }

    @Test
    void tradeOffsFollowTheirChance() {
        for (long seed = 0; seed < 50; seed++) {
            assertFalse(roll(SettlerRarity.COMMON, "builder", 0, seed).hasTrait("greedy"));
        }
        // The only trade-off open to a Builder is Greedy, so a sure trade-off always picks it.
        assertTrue(roll(SettlerRarity.COMMON, "builder", 1, 7).hasTrait("greedy"));
    }

    @Test
    void itGetsANameAHistoryAndASpecialty() {
        final Settler builder = roll(SettlerRarity.COMMON, "builder", 0, 1);

        assertEquals("Aldric Tanner", builder.getName());
        assertEquals("history.dungeon", builder.getHistory());
        assertEquals(List.of("the Sunken Keep"), builder.getHistoryArgs());
        assertTrue(List.of("mason", "smith").contains(builder.getSpecialty()));
        assertNull(roll(SettlerRarity.COMMON, "farmer", 0, 1).getSpecialty());
        assertNull(roll(SettlerRarity.COMMON, null, 0, 1).getProfession());
    }

    @Test
    void anUnknownProfessionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> roll(SettlerRarity.COMMON, "wizard", 0, 1));
    }
}
