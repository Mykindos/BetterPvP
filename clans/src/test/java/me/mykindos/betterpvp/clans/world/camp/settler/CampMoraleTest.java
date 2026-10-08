package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerDeparture;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampMoraleTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 42);
    private static final long HOUR = 3_600_000L;
    private static final long NOW = 1_000 * HOUR;

    private final SettlerConfig config = mock(SettlerConfig.class);
    private final Roster roster = new Roster();
    private int food;
    private int boost;
    private CampMorale morale;

    @BeforeEach
    void setUp() {
        when(config.morale(anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
        when(config.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));
        when(config.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.3),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)), List.of(), List.of(), Map.of()));
        final FoodSource granary = site -> food;
        final MoraleBoost bell = site -> boost;
        morale = new CampMorale(config, new CampWideTraits(config), Set.of(granary), Set.of(bell));
    }

    private Settler settler(String profession, SettlerRarity rarity, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Corin Salt");
        settler.setProfession(profession);
        settler.setRarity(rarity);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        settler.setJoinedAt(NOW - 100 * HOUR);
        settler.setStateSince(NOW - 100 * HOUR);
        roster.getSettlers().add(settler);
        return settler;
    }

    private int of(Settler settler) {
        return morale.morale(CAMP, settler, roster, NOW);
    }

    @Test
    void ac20_moraleIsNeutralWhenNothingIsHappening() {
        assertEquals(0, of(settler(null, SettlerRarity.COMMON)));
        final Settler working = settler("builder", SettlerRarity.COMMON);
        working.setState(SettlerState.WORKING);
        assertEquals(0, of(working));
        final Settler newcomer = settler("builder", SettlerRarity.COMMON);
        newcomer.setJoinedAt(NOW - HOUR);
        newcomer.setStateSince(NOW - HOUR);
        assertEquals(0, of(newcomer));
    }

    @Test
    void ac21_foodOnlyRaisesMoraleAndOnlyTheBestCounts() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        food = -15;
        assertEquals(0, of(wanderer), "food never lowers morale");

        final FoodSource granary = site -> 10;
        final FoodSource mill = site -> 20;
        morale = new CampMorale(config, new CampWideTraits(config), Set.of(granary, mill), Set.of());
        assertEquals(20, of(wanderer), "only the best food counts");

        final FoodSource feast = site -> 45;
        morale = new CampMorale(config, new CampWideTraits(config), Set.of(granary, feast), Set.of());
        assertEquals(30, of(wanderer), "food stops at its most");
    }

    @Test
    void ac21_boostsAddOnTopOfFoodAndOfEachOther() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        final FoodSource feast = site -> 30;
        final MoraleBoost bell = site -> 10;
        final MoraleBoost banner = site -> 5;
        morale = new CampMorale(config, new CampWideTraits(config), Set.of(feast), Set.of(bell, banner));
        assertEquals(45, of(wanderer));
    }

    @Test
    void ac22_anyStrikeBringsEveryoneDown() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        final Settler striker = settler("builder", SettlerRarity.COMMON);
        striker.changeState(SettlerState.STRIKING, NOW);
        assertEquals(-25, of(wanderer));
        assertEquals(-25, of(striker));
    }

    @Test
    void ac23_idleProfessionalsGrowRestlessAfterADay() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        builder.setStateSince(NOW - 30 * HOUR);
        builder.setJoinedAt(NOW - 30 * HOUR);
        assertEquals(-6, of(builder));

        builder.setStateSince(NOW - 500 * HOUR);
        builder.setJoinedAt(NOW - 500 * HOUR);
        assertEquals(-30, of(builder), "idleness bottoms out");
        builder.setState(SettlerState.WORKING);
        assertEquals(0, of(builder));
    }

    @Test
    void ac23_idleTimeCountsFromTheLaterOfJoiningAndTheLastChange() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        builder.setStateSince(NOW - 500 * HOUR);
        builder.setJoinedAt(NOW - 25 * HOUR - HOUR / 2);
        assertEquals(-1, of(builder), "one full hour past the day");
    }

    @Test
    void ac23_wanderersAndStrikersAreNeverIdle() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        wanderer.setStateSince(NOW - 500 * HOUR);
        wanderer.setJoinedAt(NOW - 500 * HOUR);
        assertEquals(0, of(wanderer));

        final Settler striker = settler("builder", SettlerRarity.COMMON);
        striker.setState(SettlerState.STRIKING);
        assertEquals(-25, of(striker), "only the unpaid penalty, no idleness");
    }

    @Test
    void ac24_dismissalsFadeAndStopAtTheirFloor() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        roster.getDepartures().add(new SettlerDeparture("Tobin", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED,
                NOW - 24 * HOUR));
        assertEquals(-5, of(wanderer));

        for (int i = 0; i < 5; i++) {
            roster.getDepartures().add(new SettlerDeparture("Hild", SettlerRarity.COMMON,
                    SettlerLeaveReason.DISMISSED, NOW));
        }
        assertEquals(-30, of(wanderer));
    }

    @Test
    void ac24_aDismissalStartsAtTenAndIsGoneAfterTwoDays() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        roster.getDepartures().add(new SettlerDeparture("Tobin", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED,
                NOW));
        assertEquals(-10, of(wanderer));
        roster.getDepartures().clear();
        roster.getDepartures().add(new SettlerDeparture("Tobin", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED,
                NOW - 48 * HOUR));
        assertEquals(0, of(wanderer));
    }

    @Test
    void ac24_onlyDismissalsCount() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        roster.getDepartures().add(new SettlerDeparture("Maud", SettlerRarity.COMMON, SettlerLeaveReason.UNHAPPY, NOW));
        roster.getDepartures().add(new SettlerDeparture("Hal", SettlerRarity.COMMON, SettlerLeaveReason.UNPAID, NOW));
        assertEquals(0, of(wanderer));
    }

    @Test
    void campWideTraitsLiftEveryoneAtTheirStrength() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        settler(null, SettlerRarity.LEGENDARY, CampTraits.BARD);
        assertEquals(10, of(wanderer));

        food = 20;
        settler(null, SettlerRarity.COMMON, CampTraits.COOK);
        assertEquals(10 + 25, of(wanderer));
        food = 40;
        assertEquals(10 + 30, of(wanderer), "food stops at its most");
    }

    @Test
    void personalTraitsShapeHowASettlerTakesIt() {
        settler("builder", SettlerRarity.COMMON).changeState(SettlerState.STRIKING, NOW);
        final Settler content = settler(null, SettlerRarity.COMMON, CampTraits.CONTENT);
        final Settler moody = settler(null, SettlerRarity.COMMON, CampTraits.MOODY);
        final Settler homesick = settler(null, SettlerRarity.COMMON, CampTraits.HOMESICK);
        final Settler loyal = settler(null, SettlerRarity.COMMON, CampTraits.LOYAL);

        assertEquals(-20, of(content));
        assertEquals(-50, of(moody));
        assertEquals(-35, of(homesick));
        assertFalse(morale.mayLeave(loyal));
    }

    @Test
    void aBelovedSettlerSoftensIdlenessAndDismissals() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        roster.getDepartures().add(new SettlerDeparture("Tobin", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED, NOW));
        settler(null, SettlerRarity.LEGENDARY, CampTraits.BELOVED);

        assertEquals(-10, of(builder));
    }
}
