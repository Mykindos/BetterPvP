package me.mykindos.betterpvp.core.world.settler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerGeneratorTest {

    private static final int ROLLS = 4_000;

    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final TraitRegistry traits = new TraitRegistry();
    private final SettlerGenerator generator = new SettlerGenerator(professions, traits);

    /** The PRD's numbers: 1, 1, 2 and 3 traits, with 30%, 25%, 15% and 5% trade-offs. */
    private static SettlerTable prdTable() {
        return new SettlerTable(
                Map.of(SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.30),
                        SettlerRarity.UNCOMMON, new RarityNumbers(1, 1.2, 1.2, 0.25),
                        SettlerRarity.RARE, new RarityNumbers(2, 1.5, 1.5, 0.15),
                        SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)),
                List.of("Aldric"), List.of("Tanner"),
                Map.of("dungeon", List.of("history.dungeon")));
    }

    private static SettlerTable table(double tradeOffChance) {
        return new SettlerTable(
                Map.of(SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, tradeOffChance),
                        SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, tradeOffChance)),
                List.of("Aldric"), List.of("Tanner"),
                Map.of("dungeon", List.of("history.dungeon")));
    }

    @BeforeEach
    void setUp() {
        professions.register(Profession.construction("builder", "builder", List.of("mason", "smith")));
        professions.register(Profession.workplace("farmer", "farmer", "farm"));
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

    private static SettlerTemplate template(SettlerRarity rarity, String profession) {
        return SettlerTemplate.builder().rarity(rarity).profession(profession).source("dungeon")
                .historyArg("the Sunken Keep").build();
    }

    private Settler roll(SettlerRarity rarity, String profession, double tradeOffChance, long seed) {
        return generator.roll(template(rarity, profession), table(tradeOffChance), new Random(seed));
    }

    private boolean isTradeOff(String trait) {
        return traits.find(trait).orElseThrow().isTradeOff();
    }

    @Test
    void ac16_aRolledSettlerHasANewIdAndTheTemplatesRarityAndProfession() {
        final Set<UUID> ids = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            final Settler settler = roll(SettlerRarity.RARE, "farmer", 0, seed);
            assertTrue(ids.add(settler.getId()));
            assertEquals(SettlerRarity.RARE, settler.getRarity());
            assertEquals("farmer", settler.getProfession());
        }
        assertNull(roll(SettlerRarity.COMMON, null, 0, 1).getProfession());
        assertThrows(IllegalArgumentException.class, () -> roll(SettlerRarity.COMMON, "wizard", 0, 1));
    }

    @Test
    void ac17_itGetsANameAndAHistoryFromTheTable() {
        final Settler builder = roll(SettlerRarity.COMMON, "builder", 0, 1);
        assertEquals("Aldric Tanner", builder.getName());
        assertEquals("history.dungeon", builder.getHistory());
        assertEquals(List.of("the Sunken Keep"), builder.getHistoryArgs());

        final SettlerTable noBynames = new SettlerTable(Map.of(), List.of("Aldric", "Wren"), List.of(),
                Map.of("dock", List.of("history.dock.a", "history.dock.b")));
        final Set<String> histories = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            final Settler settler = generator.roll(SettlerTemplate.builder().rarity(SettlerRarity.COMMON)
                    .source("dock").historyArg("Saltmere").build(), noBynames, new Random(seed));
            assertTrue(List.of("Aldric", "Wren").contains(settler.getName()));
            histories.add(settler.getHistory());
            assertEquals(List.of("Saltmere"), settler.getHistoryArgs());
        }
        assertEquals(Set.of("history.dock.a", "history.dock.b"), histories);

        final Settler unsourced = generator.roll(SettlerTemplate.builder().rarity(SettlerRarity.COMMON)
                .source("nowhere").historyArg("x").build(), noBynames, new Random(1));
        assertNull(unsourced.getHistory());
        assertTrue(unsourced.getHistoryArgs().isEmpty());
    }

    @Test
    void ac18_aProfessionalRollsOneOfItsSpecialtiesEvenly() {
        final Random random = new Random(42);
        int masons = 0;
        for (int i = 0; i < ROLLS; i++) {
            final String specialty = generator.roll(template(SettlerRarity.COMMON, "builder"), table(0), random)
                    .getSpecialty();
            assertNotNull(specialty);
            assertTrue(List.of("mason", "smith").contains(specialty));
            if (specialty.equals("mason")) {
                masons++;
            }
        }
        assertEquals(0.5, masons / (double) ROLLS, 0.04);
        assertNull(roll(SettlerRarity.COMMON, "farmer", 0, 1).getSpecialty());
        assertNull(roll(SettlerRarity.COMMON, null, 0, 1).getSpecialty());
    }

    @Test
    void ac19_rarityDecidesHowManyTraitsAndTheyNeverRepeat() {
        final Map<SettlerRarity, Integer> expected = Map.of(SettlerRarity.COMMON, 1, SettlerRarity.UNCOMMON, 1,
                SettlerRarity.RARE, 2, SettlerRarity.LEGENDARY, 3);
        for (long seed = 0; seed < 50; seed++) {
            for (Map.Entry<SettlerRarity, Integer> entry : expected.entrySet()) {
                final List<String> rolled = generator.roll(template(entry.getKey(), "builder"), prdTable(),
                        new Random(seed)).getTraits();
                assertEquals(entry.getValue(), rolled.size(), entry.getKey().name());
                assertEquals(rolled.size(), new HashSet<>(rolled).size());
            }
        }

        final TraitRegistry few = new TraitRegistry();
        few.register(trait("bard").build());
        few.register(trait("greedy").tradeOff(true).build());
        final SettlerGenerator scarce = new SettlerGenerator(professions, few);
        final List<String> rolled = scarce.roll(template(SettlerRarity.LEGENDARY, null), prdTable(), new Random(3))
                .getTraits();
        assertEquals(Set.of("bard", "greedy"), new HashSet<>(rolled), "fewer only when no more can roll");
        assertEquals(2, rolled.size());
    }

    @Test
    void ac20_traitsOnlyRollOnTheProfessionsAndRaritiesTheyAllow() {
        final Set<String> legendaryWanderer = new HashSet<>();
        final Set<String> commonFarmer = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            commonFarmer.addAll(roll(SettlerRarity.COMMON, "farmer", 0, seed).getTraits());
            legendaryWanderer.addAll(roll(SettlerRarity.LEGENDARY, null, 0, seed).getTraits());
        }
        assertEquals(Set.of("green_thumb", "bard", "loyal"), commonFarmer);
        assertEquals(Set.of("bard", "loyal", "beloved"), legendaryWanderer);
    }

    @Test
    void ac21_eachTraitIsATradeOffWithItsRaritysChance() {
        final Random random = new Random(7);
        int tradeOffs = 0;
        for (int i = 0; i < ROLLS; i++) {
            final List<String> rolled = generator.roll(template(SettlerRarity.COMMON, "builder"), prdTable(), random)
                    .getTraits();
            if (isTradeOff(rolled.getFirst())) {
                tradeOffs++;
            }
        }
        assertEquals(0.30, tradeOffs / (double) ROLLS, 0.03);

        for (long seed = 0; seed < 50; seed++) {
            assertEquals(1, roll(SettlerRarity.COMMON, "farmer", 1, seed).getTraits().size(),
                    "a wanted trade-off with none open rolls a plain trait");
        }

        final TraitRegistry onlyTradeOffs = new TraitRegistry();
        onlyTradeOffs.register(trait("greedy").tradeOff(true).build());
        final SettlerGenerator tradeOffGenerator = new SettlerGenerator(professions, onlyTradeOffs);
        assertEquals(List.of("greedy"),
                tradeOffGenerator.roll(template(SettlerRarity.COMMON, null), table(0), new Random(1)).getTraits(),
                "a wanted plain trait with none open rolls a trade-off");
    }

    @Test
    void ac22_aRarityWithNoNumbersRollsOnePlainTrait() {
        final SettlerTable table = table(1);
        assertEquals(new RarityNumbers(1, 1, 1, 0), table.rarity(SettlerRarity.UNCOMMON));
        for (long seed = 0; seed < 100; seed++) {
            final List<String> rolled = generator.roll(template(SettlerRarity.UNCOMMON, "builder"), table,
                    new Random(seed)).getTraits();
            assertEquals(1, rolled.size());
            assertFalse(isTradeOff(rolled.getFirst()));
        }
    }

    @Test
    void ac24_strengthAndStatsAreReadFromTheTableWhenUsed() {
        final Settler rare = roll(SettlerRarity.RARE, "builder", 0, 1);
        final Settler common = roll(SettlerRarity.COMMON, "builder", 0, 2);
        final SettlerTable table = new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1.0, 1.0, 0.3),
                SettlerRarity.RARE, new RarityNumbers(2, 1.5, 1.4, 0.15)), List.of(), List.of(), Map.of());

        assertEquals(1.5, table.rarity(rare.getRarity()).getTraitStrength());
        assertEquals(1.4, table.rarity(rare.getRarity()).getStats());
        assertEquals(1.0, table.rarity(common.getRarity()).getTraitStrength());
        assertEquals(1.0, table.rarity(common.getRarity()).getStats());

        final SettlerTable changed = new SettlerTable(Map.of(
                SettlerRarity.RARE, new RarityNumbers(2, 1.8, 1.6, 0.15)), List.of(), List.of(), Map.of());
        assertEquals(1.8, changed.rarity(rare.getRarity()).getTraitStrength(),
                "a settler rolled before the change gets the new strength");
        assertEquals(1.6, changed.rarity(rare.getRarity()).getStats());
    }
}
