package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The farm bonus, with the shipped settlers.yml. */
class FarmWorkplaceTest {

    private static final SiteKey SITE = Camps.keyFor(42);

    private final SettlerService settlers = mock(SettlerService.class);
    private final Roster roster = new Roster();
    private FarmWorkplace farm;

    @BeforeEach
    void setUp() {
        when(settlers.roster(SITE)).thenReturn(Optional.of(roster));
        farm = new FarmWorkplace(ShippedSettlers.config(), settlers);
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
    void ac20_anEmptyFarmHasNoBonus() {
        assertEquals(0, farm.growth(SITE));
        assertEquals(0, farm.extraDrop(SITE));
    }

    @Test
    void ac20_eachFarmerAddsItsStatsWorth() {
        farmer(SettlerRarity.COMMON);
        assertEquals(0.10, farm.growth(SITE), 1e-9);
        farmer(SettlerRarity.UNCOMMON);
        farmer(SettlerRarity.RARE);
        assertEquals(0.10 + 0.125 + 0.16, farm.extraDrop(SITE), 1e-9);
        farmer(SettlerRarity.LEGENDARY);
        assertEquals(0.10 + 0.125 + 0.16 + 0.22, farm.growth(SITE), 1e-9);
    }

    @Test
    void ac20_onlyFarmersAtWorkOnTheFarmCount() {
        final Settler idle = farmer(SettlerRarity.COMMON);
        idle.setAssignment(null);
        idle.setState(SettlerState.IDLE);
        assertEquals(0, farm.growth(SITE));
        assertEquals(List.of(), farm.farmers(SITE));

        final Settler working = farmer(SettlerRarity.COMMON);
        assertEquals(List.of(working), farm.farmers(SITE));
    }

    @Test
    void ac20_moraleAndStrikesMatterAndTheFarmIsCapped() {
        final Settler farmer = farmer(SettlerRarity.COMMON);
        farmer.setMorale(-100);
        assertEquals(0.05, farm.growth(SITE), 1e-9);
        farmer.setMorale(100);
        assertEquals(0.15, farm.growth(SITE), 1e-9);

        farmer.setState(SettlerState.STRIKING);
        assertEquals(0, farm.growth(SITE));

        for (int i = 0; i < 10; i++) {
            farmer(SettlerRarity.LEGENDARY, CampTraits.BOUNTIFUL, CampTraits.GREEN_THUMB);
        }
        assertEquals(1.0, farm.growth(SITE));
        assertEquals(0.5, farm.extraDrop(SITE));
    }

    @Test
    void ac21_theirOwnTraitAddsToOnlyItsBonus() {
        final Settler greenThumb = farmer(SettlerRarity.COMMON, CampTraits.GREEN_THUMB);
        assertEquals(0.20, farm.growth(SITE), 1e-9);
        assertEquals(0.10, farm.extraDrop(SITE), 1e-9);

        roster.getSettlers().remove(greenThumb);
        farmer(SettlerRarity.LEGENDARY, CampTraits.BOUNTIFUL);
        assertEquals(0.22, farm.growth(SITE), 1e-9);
        assertEquals(0.22 + 0.20, farm.extraDrop(SITE), 1e-9);
    }

    @Test
    void ac21_seasonedAndMoodyStrengthenEverything() {
        final Settler seasoned = farmer(SettlerRarity.COMMON, CampTraits.SEASONED);
        assertEquals(0.10 * 1.2, farm.growth(SITE), 1e-9);
        roster.getSettlers().remove(seasoned);

        final Settler moody = farmer(SettlerRarity.COMMON, CampTraits.MOODY);
        assertEquals(0.10 * 1.3, farm.growth(SITE), 1e-9);
        roster.getSettlers().remove(moody);

        farmer(SettlerRarity.COMMON, CampTraits.SEASONED, CampTraits.MOODY);
        assertEquals(0.10 * 1.5, farm.growth(SITE), 1e-9);
    }

    @Test
    void ac21_prodigyAndHomesickRaiseProfessionStats() {
        final Settler prodigy = farmer(SettlerRarity.LEGENDARY, CampTraits.PRODIGY);
        assertEquals(0.10 * 2.2 * 1.5, farm.growth(SITE), 1e-9);
        roster.getSettlers().remove(prodigy);

        farmer(SettlerRarity.COMMON, CampTraits.HOMESICK);
        assertEquals(0.11, farm.growth(SITE), 1e-9);
    }

    @Test
    void ac22_growthOverWholeStagesAlwaysGrowsThemAndTheRestIsAChance() {
        assertEquals(0, FarmBonusListener.stages(0.3, 0.5));
        assertEquals(1, FarmBonusListener.stages(0.3, 0.2));
        assertEquals(1, FarmBonusListener.stages(1.0, 0.99));
        assertEquals(2, FarmBonusListener.stages(1.5, 0.1));
        assertEquals(0, FarmBonusListener.stages(0, 0));
    }

    @Test
    void ac24_theFarmHasTheFarmerCapAsItsSlots() {
        when(settlers.workingCap(SITE, CampProfessions.FARMER)).thenReturn(OptionalInt.of(2));
        assertEquals(2, farm.slots(SITE));
        when(settlers.workingCap(SITE, CampProfessions.FARMER)).thenReturn(OptionalInt.empty());
        assertEquals(-1, farm.slots(SITE));
    }

    @Test
    void ac24_onlyIdleFarmersWithNoAssignmentCanBeSent() {
        final Settler idle = farmer(SettlerRarity.COMMON);
        idle.setAssignment(null);
        idle.setState(SettlerState.IDLE);
        farmer(SettlerRarity.COMMON);
        final Settler striking = farmer(SettlerRarity.COMMON);
        striking.setAssignment(null);
        striking.setState(SettlerState.STRIKING);
        final Settler wanderer = farmer(SettlerRarity.COMMON);
        wanderer.setProfession(null);
        wanderer.setAssignment(null);
        wanderer.setState(SettlerState.IDLE);

        assertEquals(List.of(idle), farm.idleFarmers(SITE));
    }
}
