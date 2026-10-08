package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CampProsperityTest {

    private final SettlerConfig config = mock(SettlerConfig.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final SiteInstances instances = mock(SiteInstances.class);
    private final FakeProsperityStore store = new FakeProsperityStore();
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
        when(settlers.roster(any())).thenReturn(Optional.empty());
        prosperity = new CampProsperity(config, new CampWideTraits(config), settlers, instances, store);
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

    private SiteInstance instance(SiteKey key, String world) {
        return new SiteInstance(UUID.randomUUID(), key, world, SiteInstance.State.values()[0]);
    }

    @Test
    void ac1_eachSettlerIsWorthItsRarity() {
        settler(SettlerRarity.COMMON, 0);
        settler(SettlerRarity.UNCOMMON, 0);
        settler(SettlerRarity.RARE, 0);
        settler(SettlerRarity.LEGENDARY, 0);
        assertEquals(150, prosperity.of(roster));
    }

    @Test
    void ac1_valuesAreReadFromConfigEachTime() {
        settler(SettlerRarity.RARE, 0);
        assertEquals(40, prosperity.of(roster));

        when(config.prosperityValue(SettlerRarity.RARE)).thenReturn(55);
        assertEquals(55, prosperity.of(roster));
    }

    @Test
    void ac1_everySettlerCountsWhateverItsProfessionAssignmentOrState() {
        final Settler wanderer = settler(SettlerRarity.COMMON, 0);
        wanderer.setProfession(null);
        final Settler worker = settler(SettlerRarity.COMMON, 0);
        worker.setProfession("farmer");
        worker.setAssignment("farm");
        worker.setState(SettlerState.WORKING);
        final Settler striker = settler(SettlerRarity.COMMON, 0);
        striker.setProfession("builder");
        striker.setState(SettlerState.STRIKING);

        assertEquals(30, prosperity.of(roster));
    }

    @Test
    void ac2_averageMoraleScalesItBetweenHalfAndHalfAgain() {
        settler(SettlerRarity.LEGENDARY, 100);
        assertEquals(120, prosperity.of(roster));

        roster.getSettlers().getFirst().setMorale(-100);
        assertEquals(40, prosperity.of(roster));
    }

    @Test
    void ac2_itIsTheAverageMoraleRoundedToTheNearestWhole() {
        settler(SettlerRarity.COMMON, 50);
        settler(SettlerRarity.COMMON, 0);
        settler(SettlerRarity.COMMON, -5);
        // worth 30, average morale 15, 30 * 1.075 = 32.25
        assertEquals(32, prosperity.of(roster));
    }

    @Test
    void ac3_theBestChroniclerAddsItsShare() {
        settler(SettlerRarity.LEGENDARY, 0, CampTraits.CHRONICLER);
        settler(SettlerRarity.COMMON, 0, CampTraits.CHRONICLER);
        assertEquals(Math.round(90 * 1.06), prosperity.of(roster));
    }

    @Test
    void ac3_theChroniclerComesOnTopOfMorale() {
        settler(SettlerRarity.LEGENDARY, 100, CampTraits.CHRONICLER);
        // 80 * 1.5 * (1 + 0.03 * 2)
        assertEquals(Math.round(80 * 1.5 * 1.06), prosperity.of(roster));
    }

    @Test
    void ac4_anEmptyCampHasNone() {
        assertEquals(0, prosperity.of(roster));
    }

    @Test
    void ac4_aCampWhoseRosterIsNotLoadedHasNone() {
        settler(SettlerRarity.LEGENDARY, 0);
        final SiteKey key = SiteKey.of(Camps.SITE_ID, 7);
        assertEquals(0, prosperity.of(key));

        when(settlers.roster(key)).thenReturn(Optional.of(roster));
        assertEquals(80, prosperity.of(key));
    }

    @Test
    void ac5_theFactorsAreRaritiesThenMoraleThenChronicler() {
        settler(SettlerRarity.LEGENDARY, 60, CampTraits.CHRONICLER);
        settler(SettlerRarity.COMMON, -20);
        settler(SettlerRarity.COMMON, 10);
        final ProsperityFactors factors = prosperity.factors(roster);

        assertEquals(List.of(ProsperityFactors.Kind.SETTLERS, ProsperityFactors.Kind.SETTLERS,
                        ProsperityFactors.Kind.MORALE, ProsperityFactors.Kind.CHRONICLER),
                factors.getFactors().stream().map(ProsperityFactors.Factor::getKind).toList());
        assertEquals(new ProsperityFactors.Factor(ProsperityFactors.Kind.SETTLERS, SettlerRarity.COMMON, 2, 20),
                factors.getFactors().get(0));
        assertEquals(new ProsperityFactors.Factor(ProsperityFactors.Kind.SETTLERS, SettlerRarity.LEGENDARY, 1, 80),
                factors.getFactors().get(1));
    }

    @Test
    void ac5_lowMoraleIsANegativeFactorAndNoChroniclerMeansNoChroniclerFactor() {
        settler(SettlerRarity.LEGENDARY, -100);
        settler(SettlerRarity.LEGENDARY, -100);
        final List<ProsperityFactors.Factor> factors = prosperity.factors(roster).getFactors();

        assertEquals(new ProsperityFactors.Factor(ProsperityFactors.Kind.SETTLERS, SettlerRarity.LEGENDARY, 2, 160),
                factors.get(0));
        assertEquals(new ProsperityFactors.Factor(ProsperityFactors.Kind.MORALE, null, -100, -80), factors.get(1));
        assertEquals(2, factors.size());
    }

    @Test
    void ac6_theFactorsAddUpToTheTotal() {
        settler(SettlerRarity.LEGENDARY, 60, CampTraits.CHRONICLER);
        settler(SettlerRarity.COMMON, -20);
        settler(SettlerRarity.COMMON, 10);
        final ProsperityFactors factors = prosperity.factors(roster);

        assertEquals(prosperity.of(roster), factors.getTotal());
        assertEquals(factors.getTotal(), Math.round(factors.getFactors().stream()
                .mapToDouble(ProsperityFactors.Factor::getAmount).sum()));
    }

    @Test
    void ac6_anEmptyOrUnloadedCampHasNoFactors() {
        assertEquals(new ProsperityFactors(List.of(), 0), prosperity.factors(roster));
        assertEquals(new ProsperityFactors(List.of(), 0), prosperity.factors(SiteKey.of(Camps.SITE_ID, 3)));
    }

    @Test
    void ac7_itWritesEveryTenMinutes() throws NoSuchMethodException {
        final UpdateEvent update = CampProsperity.class.getMethod("save").getAnnotation(UpdateEvent.class);
        assertEquals(600_000, update.delay());
    }

    @Test
    void ac7_itWritesOnlyLoadedCampsWithLoadedRosters() {
        settler(SettlerRarity.RARE, 0);
        final SiteKey loaded = SiteKey.of(Camps.SITE_ID, 1);
        final SiteKey worldNotLoaded = SiteKey.of(Camps.SITE_ID, 2);
        final SiteKey rosterNotLoaded = SiteKey.of(Camps.SITE_ID, 3);
        final SiteKey notACamp = SiteKey.of("dungeon", 4);
        when(instances.all()).thenReturn(List.of(
                instance(loaded, "w1"), instance(worldNotLoaded, "w2"),
                instance(rosterNotLoaded, "w3"), instance(notACamp, "w4")));
        when(settlers.roster(loaded)).thenReturn(Optional.of(roster));
        when(settlers.roster(worldNotLoaded)).thenReturn(Optional.of(roster));
        when(settlers.roster(notACamp)).thenReturn(Optional.of(roster));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            final World world = mock(World.class);
            bukkit.when(() -> Bukkit.getWorld("w1")).thenReturn(world);
            bukkit.when(() -> Bukkit.getWorld("w3")).thenReturn(world);
            bukkit.when(() -> Bukkit.getWorld("w4")).thenReturn(world);
            prosperity.save();
        }

        assertEquals(Map.of(1L, 40), store.prosperity());
    }

    @Test
    void ac8_disbandingDeletesTheRecord() {
        store.save(5, 100);
        store.save(6, 200);
        final Clan clan = mock(Clan.class);
        when(clan.getId()).thenReturn(5L);
        final ClanDisbandEvent event = mock(ClanDisbandEvent.class);
        when(event.getClan()).thenReturn(clan);

        prosperity.onDisband(event);

        assertTrue(store.find(5).isEmpty());
        assertEquals(200, store.find(6).getAsInt());
    }
}
