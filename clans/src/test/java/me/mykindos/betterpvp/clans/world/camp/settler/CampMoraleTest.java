package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerDeparture;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.morale.FoodSource;
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
    private CampMorale morale;

    @BeforeEach
    void setUp() {
        when(config.morale(anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
        when(config.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));
        when(config.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.3),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)), List.of(), List.of(), Map.of()));
        final FoodSource granary = site -> food;
        morale = new CampMorale(config, Set.of(granary));
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
    void aSettlerWithNothingHappeningIsNeutral() {
        assertEquals(0, of(settler(null, SettlerRarity.COMMON)));
    }

    @Test
    void anyStrikeBringsEveryoneDown() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        settler("builder", SettlerRarity.COMMON).changeState(SettlerState.STRIKING, NOW);
        assertEquals(-25, of(wanderer));
    }

    @Test
    void idleProfessionalsGrowRestlessAfterADay() {
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
    void dismissalsFadeAndStopAtTheirFloor() {
        final Settler wanderer = settler(null, SettlerRarity.COMMON);
        roster.getDepartures().add(new SettlerDeparture("Tobin", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED,
                NOW - 24 * HOUR));
        assertEquals(-5, of(wanderer));

        for (int i = 0; i < 5; i++) {
            roster.getDepartures().add(new SettlerDeparture("Hild", SettlerRarity.COMMON,
                    SettlerLeaveReason.DISMISSED, NOW));
        }
        assertEquals(-30, of(wanderer));
        roster.getDepartures().add(new SettlerDeparture("Maud", SettlerRarity.COMMON, SettlerLeaveReason.UNHAPPY, NOW));
        assertEquals(-30, of(wanderer), "only dismissals count");
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
