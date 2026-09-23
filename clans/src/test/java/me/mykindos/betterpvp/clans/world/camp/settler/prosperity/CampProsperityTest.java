package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampProsperityTest {

    private final SettlerConfig config = mock(SettlerConfig.class);
    private final Roster roster = new Roster();
    private CampProsperity prosperity;

    @BeforeEach
    void setUp() {
        when(config.prosperityValue(any())).thenAnswer(invocation ->
                10 << ((SettlerRarity) invocation.getArgument(0)).ordinal());
        when(config.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));
        when(config.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.3),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)), List.of(), List.of(), Map.of()));
        prosperity = new CampProsperity(config, new CampWideTraits(config), mock(SettlerService.class),
                mock(SiteInstances.class), mock(ProsperityStore.class));
    }

    private Settler settler(SettlerRarity rarity, int morale, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(rarity);
        settler.setMorale(morale);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        roster.getSettlers().add(settler);
        return settler;
    }

    @Test
    void anEmptyCampHasNone() {
        assertEquals(0, prosperity.of(roster));
    }

    @Test
    void eachSettlerIsWorthItsRarity() {
        settler(SettlerRarity.COMMON, 0);
        settler(SettlerRarity.UNCOMMON, 0);
        settler(SettlerRarity.RARE, 0);
        settler(SettlerRarity.LEGENDARY, 0);
        assertEquals(150, prosperity.of(roster));
    }

    @Test
    void averageMoraleScalesItBetweenHalfAndHalfAgain() {
        settler(SettlerRarity.LEGENDARY, 100);
        assertEquals(120, prosperity.of(roster));

        roster.getSettlers().getFirst().setMorale(-100);
        assertEquals(40, prosperity.of(roster));
    }

    @Test
    void theBestChroniclerAddsItsShare() {
        settler(SettlerRarity.LEGENDARY, 0, CampTraits.CHRONICLER);
        settler(SettlerRarity.COMMON, 0, CampTraits.CHRONICLER);
        assertEquals(Math.round(90 * 1.06), prosperity.of(roster));
    }

    @Test
    void aLeavingChroniclerNoLongerHelps() {
        settler(SettlerRarity.LEGENDARY, 0, CampTraits.CHRONICLER).changeState(SettlerState.LEAVING, 0);
        assertEquals(80, prosperity.of(roster));
    }
}
