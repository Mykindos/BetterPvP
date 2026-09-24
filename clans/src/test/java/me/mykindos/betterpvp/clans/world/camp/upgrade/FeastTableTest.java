package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleEngine;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeastTableTest {

    private static final long CLAN = 7;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(500 * HOUR);
    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final MoraleEngine moraleEngine = mock(MoraleEngine.class);
    private FeastTable feast;
    private PlacedStructure hall;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        feast = new FeastTable(new CampUpgrades(store), mock(CampConfig.class), store, moraleEngine,
                mock(ItemFactory.class), mock(ItemRegistry.class), now::get);
        hall = new PlacedStructure(UUID.randomUUID(), CampConstruction.GREAT_HALL,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        hall.getUpgrades().put(0, FeastTable.ID);
        camp.getHolding().getStructures().add(hall);
    }

    private static Map<Integer, FeastTable.Portion> food(int... pointsAndAmounts) {
        final Map<Integer, FeastTable.Portion> food = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pointsAndAmounts.length; i += 2) {
            food.put(i / 2, new FeastTable.Portion(pointsAndAmounts[i], pointsAndAmounts[i + 1]));
        }
        return food;
    }

    @Test
    void itTakesOnlyWhatTheFeastCosts() {
        assertEquals(Map.of(0, 10, 1, 4), FeastTable.take(food(5, 10, 8, 20), 80));
    }

    @Test
    void theLastStackIsRoundedUp() {
        assertEquals(Map.of(0, 3), FeastTable.take(food(8, 64), 20));
    }

    @Test
    void tooLittleFoodTakesNothing() {
        assertTrue(FeastTable.take(food(5, 10, 8, 2), 80).isEmpty());
    }

    @Test
    void aFeastLiftsEverySettlerUntilItEnds() {
        assertEquals(0, feast.morale(SITE));
        assertNull(feast.refusal(SITE));

        feast.begin(SITE);
        assertEquals(now.get() + 24 * HOUR, camp.getFeastUntil());
        assertEquals(15, feast.morale(SITE));
        verify(moraleEngine).settle(SITE);

        now.addAndGet(24 * HOUR);
        assertEquals(0, feast.morale(SITE));
    }

    @Test
    void onlyOneFeastAtATime() {
        feast.begin(SITE);
        assertEquals("clans.camp.upgrade.feast_table.already", feast.refusal(SITE));
        now.addAndGet(24 * HOUR);
        assertNull(feast.refusal(SITE));
    }

    @Test
    void aDisabledHallServesNoFeast() {
        feast.begin(SITE);
        hall.setCondition(StructureCondition.DISABLED);
        assertEquals(0, feast.morale(SITE));
        assertEquals("clans.camp.upgrade.feast_table.inactive", feast.refusal(SITE));
    }
}
