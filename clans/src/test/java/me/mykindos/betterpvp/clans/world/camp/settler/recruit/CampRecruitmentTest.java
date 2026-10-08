package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.clans.world.camp.upgrade.GuestQuarters;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampRecruitmentTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long HOUR = 3_600_000L;
    private static final String GHOST = "ghost";

    private final AtomicLong now = new AtomicLong(1_000 * HOUR);
    private final List<Event> events = new ArrayList<>();
    private final Camp camp = new Camp();
    private final Roster roster = new Roster();
    private final CampStore store = mock(CampStore.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final SettlerConfig settlerConfig = mock(SettlerConfig.class);
    private final RecruitConfig config = mock(RecruitConfig.class);
    private final Clan clan = mock(Clan.class);
    private final ClanManager clanManager = mock(ClanManager.class);
    private final CampPermissions permissions = mock(CampPermissions.class);
    private final CampCoins coins = mock(CampCoins.class);
    private final ConstructionSites sites = mock(ConstructionSites.class);
    private final StructureStatusTracker tracker = mock(StructureStatusTracker.class);
    private final SiteInstances instances = mock(SiteInstances.class);
    private final Player player = mock(Player.class);

    private MockedStatic<Bukkit> bukkit;
    private CampRecruitment recruitment;
    private GuestQuarters guestQuarters;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(settlers.roster(SITE)).thenReturn(Optional.of(roster));
        when(settlers.populationCap(SITE)).thenReturn(6);
        when(settlers.grant(any(), any())).thenAnswer(invocation -> SettlerResult.done(invocation.getArgument(1)));
        when(settlerConfig.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0),
                SettlerRarity.UNCOMMON, new RarityNumbers(1, 1.25, 1.25, 0),
                SettlerRarity.RARE, new RarityNumbers(2, 1.5, 1.6, 0),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0)), List.of("Aldric"), List.of("Tanner"),
                Map.of("dock", List.of("history.dock"), "hiring", List.of("history.hiring"),
                        "milestone", List.of("history.milestone"))));
        when(settlerConfig.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));

        when(config.getArrivalEvery()).thenReturn(Duration.ofHours(4));
        when(config.getArrivalWait()).thenReturn(Duration.ofHours(2));
        when(config.getArrivalCounts()).thenReturn(Map.of(2, 1.0));
        when(config.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.RARE, 1.0),
                Map.of(CampProfessions.BUILDER, 1.0)));
        when(config.getArrivalPrices()).thenReturn(Map.of(SettlerRarity.UNCOMMON, 4_500L, SettlerRarity.RARE, 15_000L,
                SettlerRarity.LEGENDARY, 60_000L));
        when(config.getCampWideTraitPrice()).thenReturn(2_000L);
        when(config.getBoardSize()).thenReturn(4);
        when(config.getBoardRefresh()).thenReturn(Duration.ofHours(12));
        when(config.getReroll()).thenReturn(2_000L);
        when(config.getBoardOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.COMMON, 1.0),
                Map.of(SettlerOdds.NONE, 1.0)));
        when(config.getBoardPrices()).thenReturn(Map.of(SettlerRarity.COMMON, 5_000L, SettlerRarity.UNCOMMON, 15_000L,
                SettlerRarity.RARE, 50_000L, SettlerRarity.LEGENDARY, 200_000L));
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                5, new RecruitConfig.Milestone(CampProfessions.BUILDER, SettlerRarity.COMMON),
                10, new RecruitConfig.Milestone(RecruitConfig.ANY, SettlerRarity.LEGENDARY),
                25, new RecruitConfig.Milestone(CampProfessions.FARMER, SettlerRarity.RARE))));

        when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(clan));
        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(coins.take(any(), anyLong())).thenReturn(true);

        guestQuarters = new GuestQuarters(new CampUpgrades(store), store, permissions);
        final TraitRegistry traits = new TraitRegistry();
        new CampTraits(traits);
        recruitment = recruitment(traits);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private CampRecruitment recruitment(TraitRegistry traits) {
        final ProfessionRegistry professions = new ProfessionRegistry();
        new CampProfessions(professions);
        return new CampRecruitment(store, settlers, new SettlerGenerator(professions, traits), settlerConfig,
                config, traits, sites, tracker, instances, clanManager, permissions, coins,
                new CampWideTraits(settlerConfig), guestQuarters, now::get);
    }

    private static String reason(SettlerResult result) {
        assertFalse(result.isSuccess(), "expected a refusal");
        return ((TranslatableComponent) result.getReason()).key();
    }

    private List<SettlerBoatEvent> boats() {
        return events.stream().filter(SettlerBoatEvent.class::isInstance).map(SettlerBoatEvent.class::cast).toList();
    }

    /** Schedules the first boat and lands it. */
    private void landABoat() {
        recruitment.settle(SITE, true);
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);
    }

    private static Settler settlerWith(SettlerRarity rarity, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(rarity);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        return settler;
    }

    private void fitGuestQuarters() {
        final PlacedStructure hall = new PlacedStructure(UUID.randomUUID(), CampConstruction.GREAT_HALL,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        hall.setStage(1);
        hall.getUpgrades().put(1, GuestQuarters.ID);
        camp.getHolding().getStructures().add(hall);
    }

    @Test
    void ac6_theFirstPassOnlySchedulesABoat() {
        recruitment.settle(SITE, true);
        assertTrue(camp.getArrivals().isEmpty());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt());
    }

    @Test
    void ac6_aBoatComesEveryIntervalWhileTheDockWorks() {
        recruitment.settle(SITE, true);
        now.addAndGet(4 * HOUR - 1);
        recruitment.settle(SITE, true);
        assertTrue(camp.getArrivals().isEmpty(), "not due yet");

        now.addAndGet(1);
        recruitment.settle(SITE, true);
        assertEquals(2, camp.getArrivals().size());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt());
    }

    @Test
    void ac6_noBoatsComeWhileTheDockIsBroken() {
        recruitment.settle(SITE, false);
        now.addAndGet(9 * HOUR);
        recruitment.settle(SITE, false);

        assertTrue(camp.getArrivals().isEmpty());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt());
    }

    @Test
    void ac7_aBoatBringsCandidatesFromTheOddsWhoWaitAWhile() {
        landABoat();

        assertEquals(2, camp.getArrivals().size(), "the count odds say 2");
        for (SettlerCandidate candidate : camp.getArrivals()) {
            assertEquals(SettlerRarity.RARE, candidate.getSettler().getRarity());
            assertEquals(CampProfessions.BUILDER, candidate.getSettler().getProfession());
            assertEquals("history.dock", candidate.getSettler().getHistory());
            assertEquals(now.get() + 2 * HOUR, candidate.getExpiresAt());
        }

        now.addAndGet(2 * HOUR);
        recruitment.settle(SITE, false);
        assertTrue(camp.getArrivals().isEmpty(), "they give up waiting");
    }

    @Test
    void ac8_aCommonIsFreeUnlessItHasACampWideTrait() {
        when(config.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.COMMON, 1.0),
                Map.of(SettlerOdds.NONE, 1.0)));

        final List<SettlerCandidate> plain = recruitment(new TraitRegistry()).boat(null, new Random(1));
        assertTrue(plain.stream().allMatch(candidate -> candidate.getPrice() == 0));

        final TraitRegistry bardOnly = new TraitRegistry();
        bardOnly.register(Trait.builder().id(CampTraits.BARD).key("bard").group(TraitGroup.SITE_WIDE).build());
        final List<SettlerCandidate> bards = recruitment(bardOnly).boat(null, new Random(1));
        assertTrue(bards.stream().allMatch(candidate -> candidate.getPrice() == 2_000));
    }

    @Test
    void ac8_higherRaritiesCostTheirArrivalPrice() {
        final Map<SettlerRarity, Long> expected = Map.of(SettlerRarity.UNCOMMON, 4_500L, SettlerRarity.RARE, 15_000L,
                SettlerRarity.LEGENDARY, 60_000L);
        expected.forEach((rarity, price) -> {
            when(config.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(rarity, 1.0), Map.of(SettlerOdds.NONE, 1.0)));
            assertTrue(recruitment.boat(null, new Random(1)).stream()
                    .allMatch(candidate -> candidate.getPrice() == price), rarity + " costs " + price);
        });
    }

    @Test
    void ac9_aClosedCampOnlyKeepsBoatsStillWaiting() {
        recruitment.settle(SITE, true);
        final long firstDue = camp.getNextArrivalAt();
        now.addAndGet(13 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(2, camp.getArrivals().size(), "only the boat due an hour ago is still there");
        assertEquals(firstDue + 8 * HOUR + 2 * HOUR, camp.getArrivals().getFirst().getExpiresAt(),
                "it keeps the leave time of the third boat, the one it came on");
        assertTrue(camp.getNextArrivalAt() > now.get());
        assertEquals(1, boats().size());
        assertFalse(boats().getFirst().isMilestone());
    }

    @Test
    void ac9_atMostSixBoatsAreCaughtUp() {
        when(config.getArrivalWait()).thenReturn(Duration.ofHours(100));
        recruitment.settle(SITE, true);
        final long firstDue = camp.getNextArrivalAt();
        now.addAndGet(40 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(6, boats().size());
        assertTrue(boats().stream().noneMatch(SettlerBoatEvent::isMilestone));
        assertEquals(12, camp.getArrivals().size());
        assertEquals(firstDue + 100 * HOUR, camp.getArrivals().getFirst().getExpiresAt());
        assertTrue(camp.getNextArrivalAt() > now.get());
    }

    @Test
    void ac10_anUnknownProfessionRollsAsNoProfession() {
        when(config.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.RARE, 1.0), Map.of(GHOST, 1.0)));
        when(config.getBoardOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.COMMON, 1.0), Map.of(GHOST, 1.0)));
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                5, new RecruitConfig.Milestone(GHOST, SettlerRarity.COMMON))));
        when(clan.getLevel()).thenReturn(5L);

        assertDoesNotThrow(() -> landABoat());
        assertEquals(Set.of(5), camp.getMilestones(), "the milestone counts as sent");
        assertEquals(3, camp.getArrivals().size(), "the milestone settler and the boat");
        assertTrue(camp.getArrivals().stream().allMatch(candidate -> candidate.getSettler().getProfession() == null));

        final List<SettlerCandidate> board = assertDoesNotThrow(() -> recruitment.board(SITE));
        assertEquals(4, board.size());
        assertTrue(board.stream().allMatch(candidate -> candidate.getSettler().getProfession() == null));
    }

    @Test
    void ac10_oneCampsBadRollDoesNotStopTheOthers() {
        final long otherClan = 43;
        final SiteKey other = Camps.keyFor(otherClan);
        final Camp otherCamp = new Camp();
        when(store.cached(otherClan)).thenReturn(Optional.of(otherCamp));
        when(clanManager.getClanById(otherClan)).thenReturn(Optional.empty());
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                5, new RecruitConfig.Milestone(GHOST, SettlerRarity.COMMON))));
        when(clan.getLevel()).thenReturn(5L);

        final World world = mock(World.class);
        bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(world);
        when(instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), SITE, "camp-42", SiteInstance.State.READY),
                new SiteInstance(UUID.randomUUID(), other, "camp-43", SiteInstance.State.READY)));

        assertDoesNotThrow(() -> recruitment.tick());
        assertEquals(now.get() + 4 * HOUR, otherCamp.getNextArrivalAt(), "the second camp was settled too");
    }

    @Test
    void ac10_aCampThatFailsToSettleDoesNotStopTheOthers() {
        final long brokenClan = 43;
        when(store.cached(brokenClan)).thenThrow(new IllegalStateException("broken camp"));

        final World world = mock(World.class);
        bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(world);
        when(instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), Camps.keyFor(brokenClan), "camp-43", SiteInstance.State.READY),
                new SiteInstance(UUID.randomUUID(), SITE, "camp-42", SiteInstance.State.READY)));

        assertDoesNotThrow(() -> recruitment.tick());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt(), "the camp after the broken one was settled");
    }

    @Test
    void ac11_aPassWithNothingToDoWritesNothing() {
        recruitment.settle(SITE, true);
        verify(store).changed(CLAN);
        clearInvocations(store);

        recruitment.settle(SITE, true);
        now.addAndGet(HOUR);
        recruitment.settle(SITE, true);
        recruitment.settle(SITE, false);
        verify(store, never()).changed(anyLong());
    }

    @Test
    void ac11_aPassThatChangesSomethingIsWrittenDown() {
        recruitment.settle(SITE, true);
        clearInvocations(store);
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);
        verify(store).changed(CLAN);
    }

    @Test
    void ac11_aPassWhereOnlyACandidateLeavesIsWrittenDown() {
        landABoat();
        clearInvocations(store);
        now.addAndGet(2 * HOUR);
        final long nextBoat = camp.getNextArrivalAt();

        recruitment.settle(SITE, true);
        assertTrue(camp.getArrivals().isEmpty());
        assertEquals(nextBoat, camp.getNextArrivalAt(), "no boat was rescheduled");
        verify(store).changed(CLAN);
    }

    @Test
    void ac11_aPassWhereOnlyAMilestoneIsSentIsWrittenDown() {
        recruitment.settle(SITE, true);
        clearInvocations(store);
        final long nextBoat = camp.getNextArrivalAt();
        when(clan.getLevel()).thenReturn(5L);

        recruitment.settle(SITE, true);
        assertEquals(1, camp.getArrivals().size());
        assertEquals(nextBoat, camp.getNextArrivalAt(), "no boat was rescheduled");
        verify(store).changed(CLAN);
    }

    @Test
    void ac12_aBoatRolledAheadLandsAsItWasSeen() {
        recruitment.settle(SITE, true);
        final List<SettlerCandidate> seen = recruitment.boat(null, new Random(1));
        assertTrue(seen.stream().allMatch(candidate -> candidate.getExpiresAt() == 0), "no time limit before landing");
        camp.setNextBoat(new ArrayList<>(seen));
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(seen, camp.getArrivals());
        assertEquals(now.get() + 2 * HOUR, camp.getArrivals().getFirst().getExpiresAt());
        assertTrue(camp.getNextBoat().isEmpty());
    }

    @Test
    void ac13_aChosenProfessionIsGivenToEveryoneOnTheNextBoatOnly() {
        recruitment.settle(SITE, true);
        camp.setNextBoatProfession(CampProfessions.FARMER);
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(2, camp.getArrivals().size());
        assertTrue(camp.getArrivals().stream()
                .allMatch(candidate -> CampProfessions.FARMER.equals(candidate.getSettler().getProfession())));
        assertNull(camp.getNextBoatProfession());

        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);
        assertTrue(camp.getArrivals().stream()
                .allMatch(candidate -> CampProfessions.BUILDER.equals(candidate.getSettler().getProfession())),
                "the next boat rolls from the arrival odds again");
    }

    @Test
    void ac14_eachMilestoneSendsItsSettlerFreeWithNoTimeLimit() {
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                5, new RecruitConfig.Milestone(CampProfessions.BUILDER, SettlerRarity.UNCOMMON),
                10, new RecruitConfig.Milestone(CampProfessions.BUILDER, SettlerRarity.RARE),
                25, new RecruitConfig.Milestone(CampProfessions.FARMER, SettlerRarity.RARE),
                50, new RecruitConfig.Milestone(RecruitConfig.ANY, SettlerRarity.LEGENDARY),
                100, new RecruitConfig.Milestone(CampProfessions.BUILDER, SettlerRarity.LEGENDARY))));
        when(clan.getLevel()).thenReturn(100L);
        recruitment.settle(SITE, false);

        final List<SettlerCandidate> sent = camp.getArrivals();
        assertEquals(5, sent.size());
        assertTrue(sent.stream().allMatch(candidate -> candidate.getPrice() == 0 && candidate.getExpiresAt() == 0));
        assertTrue(sent.stream().allMatch(candidate -> "history.milestone".equals(candidate.getSettler().getHistory())));
        assertEquals(List.of(SettlerRarity.UNCOMMON, SettlerRarity.RARE, SettlerRarity.RARE, SettlerRarity.LEGENDARY,
                SettlerRarity.LEGENDARY), sent.stream().map(candidate -> candidate.getSettler().getRarity()).toList());
        assertEquals(List.of(CampProfessions.BUILDER, CampProfessions.BUILDER, CampProfessions.FARMER,
                CampProfessions.BUILDER, CampProfessions.BUILDER),
                sent.stream().map(candidate -> candidate.getSettler().getProfession()).toList(),
                "'any' rolls from the arrival odds, which only hold Builders here");
    }

    @Test
    void ac14_noneGivesNoProfession() {
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                1, new RecruitConfig.Milestone(SettlerOdds.NONE, SettlerRarity.COMMON))));
        when(clan.getLevel()).thenReturn(1L);
        recruitment.settle(SITE, false);

        assertEquals(1, camp.getArrivals().size());
        assertNull(camp.getArrivals().getFirst().getSettler().getProfession());
    }

    @Test
    void ac15_milestonesAreSentOnceWhetherOrNotTheDockWorks() {
        when(clan.getLevel()).thenReturn(12L);
        recruitment.settle(SITE, false);

        assertEquals(2, camp.getArrivals().size(), "levels 5 and 10 at once");
        assertEquals(Set.of(5, 10), camp.getMilestones());
        assertEquals(1, boats().size());
        assertTrue(boats().getFirst().isMilestone());

        recruitment.settle(SITE, false);
        assertEquals(2, camp.getArrivals().size(), "never sent again");
        assertEquals(1, boats().size());

        when(clan.getLevel()).thenReturn(30L);
        recruitment.settle(SITE, true);
        assertEquals(3, camp.getArrivals().size());
        assertEquals(Set.of(5, 10, 25), camp.getMilestones());
    }

    @Test
    void ac16_theBoardIsRolledFromTheHiringOddsAndPrices() {
        when(config.getBoardOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.RARE, 1.0),
                Map.of(CampProfessions.FARMER, 1.0)));
        final List<SettlerCandidate> board = recruitment.board(SITE);

        assertEquals(4, board.size());
        for (SettlerCandidate candidate : board) {
            assertEquals(SettlerRarity.RARE, candidate.getSettler().getRarity());
            assertEquals(CampProfessions.FARMER, candidate.getSettler().getProfession());
            assertEquals("history.hiring", candidate.getSettler().getHistory());
            assertEquals(50_000, candidate.getPrice());
            assertEquals(0, candidate.getExpiresAt());
        }
    }

    @Test
    void ac16_theBoardTurnsOverOnItsOwnAfterTwelveHours() {
        final List<SettlerCandidate> first = List.copyOf(recruitment.board(SITE));
        now.addAndGet(12 * HOUR - 1);
        assertEquals(first, recruitment.board(SITE));

        now.addAndGet(1);
        assertNotEquals(first, recruitment.board(SITE));
        assertEquals(now.get() + 12 * HOUR, recruitment.boardRefreshAt(SITE));
    }

    @Test
    void ac17_aRerollChargesTheFeeAndRestartsTheClock() {
        roster.getSettlers().add(settlerWith(SettlerRarity.LEGENDARY, CampTraits.HAGGLER));
        final List<SettlerCandidate> first = List.copyOf(recruitment.board(SITE));
        now.addAndGet(5 * HOUR);

        assertNull(recruitment.reroll(player, SITE));
        verify(coins).take(player, 2_000);
        assertNotEquals(first, recruitment.board(SITE));
        assertEquals(now.get() + 12 * HOUR, recruitment.boardRefreshAt(SITE));
    }

    @Test
    void ac17_aRerollNeedsPermissionAndCoins() {
        final List<SettlerCandidate> first = List.copyOf(recruitment.board(SITE));

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        assertEquals("clans.settler.card.not_allowed", recruitment.reroll(player, SITE));
        verify(coins, never()).take(any(), anyLong());
        assertEquals(first, recruitment.board(SITE));

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(coins.take(any(), anyLong())).thenReturn(false);
        assertEquals("clans.settler.recruit.cannot_afford_reroll", recruitment.reroll(player, SITE));
        assertEquals(first, recruitment.board(SITE));
    }

    @Test
    void ac18_aReservedCandidateStaysThroughRerollsAndRefreshes() {
        fitGuestQuarters();
        final SettlerCandidate kept = recruitment.board(SITE).get(2);
        assertNull(guestQuarters.toggle(player, SITE, kept.getSettler().getId()));

        assertNull(recruitment.reroll(player, SITE));
        assertTrue(recruitment.board(SITE).contains(kept));
        assertEquals(4, recruitment.board(SITE).size());

        now.addAndGet(12 * HOUR);
        assertTrue(recruitment.board(SITE).contains(kept));
        assertEquals(4, recruitment.board(SITE).size());
        assertTrue(guestQuarters.isReserved(SITE, kept));
    }

    @Test
    void ac18_anUnreservedHiredOrTurnedAwayCandidateIsNoLongerKept() {
        fitGuestQuarters();
        final UUID hired = recruitment.board(SITE).getFirst().getSettler().getId();
        assertNull(guestQuarters.toggle(player, SITE, hired));
        assertNull(guestQuarters.toggle(player, SITE, hired));
        assertNull(camp.getReservedCandidate());

        assertNull(guestQuarters.toggle(player, SITE, hired));
        assertTrue(recruitment.hire(player, SITE, hired).isSuccess());
        assertNull(recruitment.reroll(player, SITE));
        assertNull(camp.getReservedCandidate());

        final UUID turned = recruitment.board(SITE).getFirst().getSettler().getId();
        assertNull(guestQuarters.toggle(player, SITE, turned));
        assertTrue(recruitment.turnAway(player, SITE, turned).isSuccess());
        assertNull(recruitment.reroll(player, SITE));
        assertNull(camp.getReservedCandidate());
    }

    @Test
    void ac18_withoutTheUpgradeAReservationIsDropped() {
        final SettlerCandidate candidate = recruitment.board(SITE).getFirst();

        assertEquals("clans.camp.upgrade.guest_quarters.inactive",
                guestQuarters.toggle(player, SITE, candidate.getSettler().getId()));
        camp.setReservedCandidate(candidate.getSettler().getId());
        assertNull(recruitment.reroll(player, SITE));
        assertFalse(recruitment.board(SITE).contains(candidate));
        assertNull(camp.getReservedCandidate());
    }

    @Test
    void ac19_aCandidateNotThereOrExpiredIsGone() {
        assertEquals("clans.settler.recruit.gone", reason(recruitment.hire(player, SITE, UUID.randomUUID())));

        landABoat();
        final UUID waiting = camp.getArrivals().getFirst().getSettler().getId();
        now.addAndGet(2 * HOUR);
        assertEquals("clans.settler.recruit.gone", reason(recruitment.hire(player, SITE, waiting)));
        verify(coins, never()).take(any(), anyLong());
    }

    @Test
    void ac19_refusalsComeInOrderAndTakeNothing() {
        final UUID id = recruitment.board(SITE).getFirst().getSettler().getId();

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        when(settlers.populationCap(SITE)).thenReturn(0);
        when(coins.take(any(), anyLong())).thenReturn(false);
        assertEquals("clans.settler.card.not_allowed", reason(recruitment.hire(player, SITE, id)));

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        assertEquals("core.settler.population_full", reason(recruitment.hire(player, SITE, id)));
        verify(coins, never()).take(any(), anyLong());

        when(settlers.populationCap(SITE)).thenReturn(6);
        assertEquals("clans.settler.recruit.cannot_afford", reason(recruitment.hire(player, SITE, id)));

        verify(coins, never()).give(any(), anyLong());
        verify(settlers, never()).grant(any(), any());
        assertTrue(recruitment.find(SITE, id).isPresent());
    }

    @Test
    void ac20_aHirePaysTheDiscountedPriceJoinsAndRemovesTheCandidate() {
        roster.getSettlers().add(settlerWith(SettlerRarity.LEGENDARY, CampTraits.HAGGLER));
        final SettlerCandidate candidate = recruitment.board(SITE).getFirst();
        final UUID id = candidate.getSettler().getId();

        assertTrue(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins).take(player, 4_000);
        verify(settlers).grant(SITE, candidate.getSettler());
        assertTrue(recruitment.find(SITE, id).isEmpty());
        assertFalse(camp.getHiringBoard().contains(candidate));

        final SettlerHiredEvent hired = events.stream().filter(SettlerHiredEvent.class::isInstance)
                .map(SettlerHiredEvent.class::cast).findFirst().orElseThrow();
        assertEquals(4_000, hired.getPrice());
        assertEquals(candidate.getSettler(), hired.getSettler());
        assertEquals(SITE, hired.getSite());
    }

    @Test
    void ac20_aDockCandidateLeavesTheDockWhenHired() {
        landABoat();
        final UUID id = camp.getArrivals().getFirst().getSettler().getId();

        assertTrue(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins).take(player, 15_000);
        assertEquals(1, camp.getArrivals().size());
    }

    @Test
    void ac20_aRefusedGrantHandsTheCoinsBack() {
        final UUID id = recruitment.board(SITE).getFirst().getSettler().getId();
        when(settlers.grant(any(), any())).thenReturn(SettlerResult.refused("core.settler.already_granted"));

        assertFalse(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins).give(player, 5_000);
        assertTrue(recruitment.find(SITE, id).isPresent(), "a hire that fell through leaves them waiting");
        assertTrue(events.stream().noneMatch(SettlerHiredEvent.class::isInstance));
    }

    @Test
    void ac21_aFreeCandidateStillNeedsAMemberAndRoom() {
        when(clan.getLevel()).thenReturn(5L);
        recruitment.settle(SITE, true);
        now.addAndGet(5 * HOUR);
        recruitment.settle(SITE, true);
        verify(settlers, never()).grant(any(), any());

        final UUID id = camp.getArrivals().getFirst().getSettler().getId();
        assertEquals(0, recruitment.price(SITE, camp.getArrivals().getFirst()));

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        assertEquals("clans.settler.card.not_allowed", reason(recruitment.hire(player, SITE, id)));

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(settlers.populationCap(SITE)).thenReturn(0);
        assertEquals("core.settler.population_full", reason(recruitment.hire(player, SITE, id)));

        when(settlers.populationCap(SITE)).thenReturn(6);
        assertTrue(recruitment.hire(player, SITE, id).isSuccess());
    }

    @Test
    void ac22_turningAwayNeedsPermissionAndCostsNothing() {
        final UUID id = recruitment.board(SITE).getFirst().getSettler().getId();

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        assertEquals("clans.settler.card.not_allowed", reason(recruitment.turnAway(player, SITE, id)));
        assertTrue(recruitment.find(SITE, id).isPresent());

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        assertTrue(recruitment.turnAway(player, SITE, id).isSuccess());
        assertTrue(recruitment.find(SITE, id).isEmpty());
        assertEquals("clans.settler.recruit.gone", reason(recruitment.turnAway(player, SITE, id)));

        landABoat();
        final UUID waiting = camp.getArrivals().getFirst().getSettler().getId();
        assertTrue(recruitment.turnAway(player, SITE, waiting).isSuccess());
        assertEquals(1, camp.getArrivals().size());

        verify(coins, never()).take(any(), anyLong());
        verify(coins, never()).give(any(), anyLong());
        verify(settlers, never()).grant(any(), any());
    }

    @Test
    void ac23_aCampThatIsNotLoadedRefusesEverythingAsNotLoaded() {
        final SiteKey closed = Camps.keyFor(99);
        final UUID id = UUID.randomUUID();

        assertEquals("core.settler.not_loaded", reason(recruitment.hire(player, closed, id)));
        assertEquals("core.settler.not_loaded", reason(recruitment.turnAway(player, closed, id)));
        assertEquals("core.settler.not_loaded", recruitment.reroll(player, closed));
    }

    @Test
    void ac23_aRosterThatIsNotLoadedRefusesAHireAsNotLoaded() {
        final UUID id = recruitment.board(SITE).getFirst().getSettler().getId();
        when(settlers.roster(SITE)).thenReturn(Optional.empty());

        assertEquals("core.settler.not_loaded", reason(recruitment.hire(player, SITE, id)));
        verify(coins, never()).take(any(), anyLong());
    }

    @Test
    void ac24_recruitersAndHagglersHelpByTheirStrength() {
        roster.getSettlers().add(settlerWith(SettlerRarity.LEGENDARY, CampTraits.RECRUITER, CampTraits.HAGGLER));

        assertEquals((long) (4 * HOUR * 0.7), recruitment.interval(SITE));
        assertEquals(4_000, recruitment.price(SITE, new SettlerCandidate(roster.getSettlers().getFirst(), 5_000, 0)));
    }

    @Test
    void ac24_onlyTheStrongestCountsAndPricesRound() {
        roster.getSettlers().add(settlerWith(SettlerRarity.COMMON, CampTraits.RECRUITER, CampTraits.HAGGLER));
        assertEquals(4_499, recruitment.price(SITE, new SettlerCandidate(new Settler(), 4_999, 0)),
                "4,999 less 10% is 4,499.1");

        roster.getSettlers().add(settlerWith(SettlerRarity.LEGENDARY, CampTraits.RECRUITER));
        assertEquals((long) (4 * HOUR * 0.7), recruitment.interval(SITE), "0.3, not 0.15 + 0.3");
    }

    @Test
    void ac24_neverPastNinetyPercentAndFreeStaysFree() {
        when(settlerConfig.trait(eq(CampTraits.RECRUITER), anyString(), anyDouble())).thenReturn(0.6);
        when(settlerConfig.trait(eq(CampTraits.HAGGLER), anyString(), anyDouble())).thenReturn(0.6);
        roster.getSettlers().add(settlerWith(SettlerRarity.LEGENDARY, CampTraits.RECRUITER, CampTraits.HAGGLER));

        assertEquals((long) (4 * HOUR * (1 - 0.9)), recruitment.interval(SITE));
        assertEquals(500, recruitment.price(SITE, new SettlerCandidate(new Settler(), 5_000, 0)));
        assertEquals(0, recruitment.price(SITE, new SettlerCandidate(new Settler(), 0, 0)));
    }
}
