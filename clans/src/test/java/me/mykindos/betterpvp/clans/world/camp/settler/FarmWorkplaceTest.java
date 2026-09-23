package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FarmWorkplaceTest {

    private static final SiteKey SITE = Camps.keyFor(42);

    private final SettlerConfig config = mock(SettlerConfig.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final Roster roster = new Roster();
    private FarmWorkplace farm;

    @BeforeEach
    void setUp() {
        when(config.farm(anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
        when(config.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));
        when(config.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.3),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)), List.of(), List.of(), Map.of()));
        when(settlers.roster(SITE)).thenReturn(Optional.of(roster));
        farm = new FarmWorkplace(config, settlers);
    }

    private Settler farmer(SettlerRarity rarity, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setProfession(CampProfessions.FARMER);
        settler.setRarity(rarity);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        settler.setAssignment(CampGrounds.FARM);
        settler.setState(SettlerState.WORKING);
        roster.getSettlers().add(settler);
        return settler;
    }

    @Test
    void anEmptyFarmHasNoBonus() {
        assertEquals(0, farm.growth(SITE));
        assertEquals(0, farm.extraDrop(SITE));
    }

    @Test
    void eachFarmerAddsItsStatsWorth() {
        farmer(SettlerRarity.COMMON);
        farmer(SettlerRarity.LEGENDARY);
        assertEquals(0.10 + 0.22, farm.growth(SITE), 1e-9);
        assertEquals(0.10 + 0.22, farm.extraDrop(SITE), 1e-9);
    }

    @Test
    void theirOwnTraitAddsToOnlyItsBonus() {
        farmer(SettlerRarity.COMMON, CampTraits.GREEN_THUMB);
        assertEquals(0.20, farm.growth(SITE), 1e-9);
        assertEquals(0.10, farm.extraDrop(SITE), 1e-9);
    }

    @Test
    void seasonedAndMoodyStrengthenEverything() {
        farmer(SettlerRarity.COMMON, CampTraits.SEASONED, CampTraits.MOODY);
        assertEquals(0.10 * 1.5, farm.growth(SITE), 1e-9);
    }

    @Test
    void moraleAndStrikesMatterAndTheFarmIsCapped() {
        final Settler unhappy = farmer(SettlerRarity.COMMON);
        unhappy.setMorale(-100);
        assertEquals(0.05, farm.growth(SITE), 1e-9);

        unhappy.setState(SettlerState.STRIKING);
        assertEquals(0, farm.growth(SITE));

        for (int i = 0; i < 10; i++) {
            farmer(SettlerRarity.LEGENDARY, CampTraits.BOUNTIFUL);
        }
        assertEquals(1.0, farm.growth(SITE));
        assertEquals(0.5, farm.extraDrop(SITE));
    }

    @Test
    void growthOverWholeStagesAlwaysGrowsThemAndTheRestIsAChance() {
        assertEquals(0, FarmBonusListener.stages(0.3, 0.5));
        assertEquals(1, FarmBonusListener.stages(0.3, 0.2));
        assertEquals(1, FarmBonusListener.stages(1.0, 0.99));
        assertEquals(2, FarmBonusListener.stages(1.5, 0.1));
    }
}
