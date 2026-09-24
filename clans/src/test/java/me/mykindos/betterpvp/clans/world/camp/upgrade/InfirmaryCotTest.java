package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.effects.EffectManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class InfirmaryCotTest {

    private static final long HOUR = 3_600_000L;

    private final CampUpgrades upgrades = new CampUpgrades(mock(CampStore.class));
    private final InfirmaryCot cot = new InfirmaryCot(mock(Clans.class), mock(ClanManager.class),
            mock(CampConfig.class), upgrades, mock(EffectManager.class));

    @Test
    void theBarracksOffersItAtItsSecondStage() {
        assertEquals(List.of(new CampUpgrades.Declared(InfirmaryCot.ID, 1)), upgrades.declared(CampStructures.BARRACKS));
    }

    @Test
    void aMemberIsTendedOncePerHour() {
        final UUID member = UUID.randomUUID();
        assertTrue(cot.tend(member, 10 * HOUR, HOUR));
        assertFalse(cot.tend(member, 10 * HOUR + HOUR / 2, HOUR));
        assertTrue(cot.tend(member, 11 * HOUR, HOUR));
    }

    @Test
    void membersAreTendedApart() {
        assertTrue(cot.tend(UUID.randomUUID(), HOUR, HOUR));
        assertTrue(cot.tend(UUID.randomUUID(), HOUR, HOUR));
    }
}
