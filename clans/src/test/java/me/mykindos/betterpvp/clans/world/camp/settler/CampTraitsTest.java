package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.WorkplaceKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Who can roll which of the camp's traits, and the camp's professions. */
class CampTraitsTest {

    private static final List<String> BUILDER_TRAITS = List.of(CampTraits.STEADY_HANDS, CampTraits.TIRELESS,
            CampTraits.FRUGAL, CampTraits.FOREMAN, CampTraits.QUICK_STUDY, CampTraits.PATCHER,
            CampTraits.ARCHITECTS_EYE, CampTraits.LONER, CampTraits.GREEDY, CampTraits.BRAWNY);
    private static final List<String> RESIDENT_TRAITS = List.of(CampTraits.GREEN_THUMB, CampTraits.BOUNTIFUL,
            CampTraits.SEASONED, CampTraits.MOODY);
    private static final List<String> ANYONE_TRAITS = List.of(CampTraits.BARD, CampTraits.RECRUITER,
            CampTraits.HAGGLER, CampTraits.QUARTERMASTER, CampTraits.CHRONICLER, CampTraits.COOK, CampTraits.LOOKOUT,
            CampTraits.BELOVED, CampTraits.LOYAL, CampTraits.CONTENT);
    private static final List<String> PROFESSIONAL_TRAITS = List.of(CampTraits.PRODIGY, CampTraits.HOMESICK);

    private final TraitRegistry registry = new TraitRegistry();

    CampTraitsTest() {
        new CampTraits(registry);
    }

    private Trait trait(String id) {
        return registry.find(id).orElseThrow(() -> new AssertionError(id + " is not registered"));
    }

    private boolean rolls(String id, String profession) {
        return trait(id).canRoll(SettlerRarity.LEGENDARY, profession);
    }

    @Test
    void ac29_eachTraitRollsOnlyOnTheSettlersItSuits() {
        assertEquals(BUILDER_TRAITS.size() + RESIDENT_TRAITS.size() + ANYONE_TRAITS.size() + PROFESSIONAL_TRAITS.size(),
                registry.all().size());

        for (String id : BUILDER_TRAITS) {
            assertTrue(rolls(id, CampProfessions.BUILDER), id);
            assertFalse(rolls(id, CampProfessions.FARMER), id);
            assertFalse(rolls(id, null), id);
        }
        for (String id : RESIDENT_TRAITS) {
            assertTrue(rolls(id, CampProfessions.FARMER), id);
            assertFalse(rolls(id, CampProfessions.BUILDER), id);
            assertFalse(rolls(id, null), id);
        }
        for (String id : ANYONE_TRAITS) {
            assertTrue(rolls(id, CampProfessions.BUILDER), id);
            assertTrue(rolls(id, CampProfessions.FARMER), id);
            assertTrue(rolls(id, null), id);
        }
        for (String id : PROFESSIONAL_TRAITS) {
            assertTrue(rolls(id, CampProfessions.BUILDER), id);
            assertTrue(rolls(id, CampProfessions.FARMER), id);
            assertFalse(rolls(id, null), id);
        }
    }

    @Test
    void ac30_someTraitsOnlyRollOnRarerSettlers() {
        final Map<String, SettlerRarity> least = Map.of(CampTraits.LOOKOUT, SettlerRarity.RARE,
                CampTraits.BELOVED, SettlerRarity.LEGENDARY, CampTraits.PRODIGY, SettlerRarity.LEGENDARY);
        for (Trait trait : registry.all()) {
            final SettlerRarity expected = least.getOrDefault(trait.getId(), SettlerRarity.COMMON);
            final String profession = trait.getProfessions().isEmpty() ? null : trait.getProfessions().iterator().next();
            assertTrue(trait.canRoll(expected, profession), trait.getId());
            if (expected != SettlerRarity.COMMON) {
                assertFalse(trait.canRoll(SettlerRarity.values()[expected.ordinal() - 1], profession), trait.getId());
            }
        }
        assertFalse(trait(CampTraits.LOOKOUT).canRoll(SettlerRarity.UNCOMMON, null));
        assertTrue(trait(CampTraits.LOOKOUT).canRoll(SettlerRarity.RARE, null));
    }

    @Test
    void ac30_theTradeOffsAreThePrds() {
        final Set<String> tradeOffs = registry.all().stream().filter(Trait::isTradeOff).map(Trait::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(CampTraits.LONER, CampTraits.GREEDY, CampTraits.BRAWNY, CampTraits.MOODY,
                CampTraits.HOMESICK), tradeOffs);
    }

    @Test
    void ac31_buildersHaveATradeAndFarmersWorkTheFarm() {
        final ProfessionRegistry professions = new ProfessionRegistry();
        new CampProfessions(professions);

        final Profession builder = professions.find(CampProfessions.BUILDER).orElseThrow();
        assertEquals(WorkplaceKind.CONSTRUCTION, builder.getWorkplaceKind());
        assertEquals(Set.of(CampProfessions.MASON, CampProfessions.CARPENTER, CampProfessions.SMITH,
                CampProfessions.LABORER), Set.copyOf(builder.getSpecialties()));

        final Profession farmer = professions.find(CampProfessions.FARMER).orElseThrow();
        assertEquals(WorkplaceKind.WORKPLACE, farmer.getWorkplaceKind());
        assertEquals(CampGrounds.FARM, farmer.getWorkplace());
        assertTrue(farmer.getSpecialties().isEmpty());
        assertEquals(2, professions.all().size());
    }
}
