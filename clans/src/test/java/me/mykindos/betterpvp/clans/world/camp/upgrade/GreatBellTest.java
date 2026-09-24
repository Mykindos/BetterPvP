package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleEngine;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GreatBellTest {

    private static final long CLAN = 7;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(500 * HOUR);
    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final MoraleEngine moraleEngine = mock(MoraleEngine.class);
    private GreatBell bell;
    private PlacedStructure hall;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        bell = new GreatBell(new CampUpgrades(store), mock(CampConfig.class), store, moraleEngine,
                () -> null, mock(StructureShapes.class), mock(SiteInstances.class),
                mock(ClanManager.class), now::get);
        hall = new PlacedStructure(UUID.randomUUID(), CampConstruction.GREAT_HALL,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        hall.getUpgrades().put(2, GreatBell.ID);
        camp.getHolding().getStructures().add(hall);
    }

    @Test
    void ringingLiftsEverySettlerForAWhile() {
        assertEquals(0, bell.morale(SITE));

        assertNull(bell.ring(SITE));
        assertEquals(now.get(), camp.getBellRungAt());
        assertEquals(10, bell.morale(SITE));
        verify(moraleEngine).settle(SITE);

        now.addAndGet(2 * HOUR);
        assertEquals(0, bell.morale(SITE));
    }

    @Test
    void itRingsOncePerCooldown() {
        assertNull(bell.ring(SITE));
        now.addAndGet(23 * HOUR);
        assertEquals("clans.camp.upgrade.great_bell.spent", bell.ring(SITE));
        assertEquals(HOUR, bell.readyIn(SITE));

        now.addAndGet(HOUR);
        assertNull(bell.ring(SITE));
    }

    @Test
    void aDisabledHallHasNoBell() {
        hall.setCondition(StructureCondition.DISABLED);
        assertEquals("clans.camp.upgrade.great_bell.inactive", bell.ring(SITE));
        assertEquals(0, camp.getBellRungAt());
    }
}
