package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
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
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampRecruitmentTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(1_000 * HOUR);
    private final Camp camp = new Camp();
    private final Roster roster = new Roster();
    private final CampStore store = mock(CampStore.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final SettlerConfig settlerConfig = mock(SettlerConfig.class);
    private final RecruitConfig config = mock(RecruitConfig.class);
    private final Clan clan = mock(Clan.class);
    private final CampPermissions permissions = mock(CampPermissions.class);
    private final CampCoins coins = mock(CampCoins.class);
    private final Player player = mock(Player.class);

    private MockedStatic<Bukkit> bukkit;
    private CampRecruitment recruitment;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(settlers.roster(SITE)).thenReturn(Optional.of(roster));
        when(settlers.populationCap(SITE)).thenReturn(6);
        when(settlers.grant(any(), any())).thenAnswer(invocation -> SettlerResult.done(invocation.getArgument(1)));
        when(settlerConfig.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0),
                SettlerRarity.RARE, new RarityNumbers(2, 1.5, 1.6, 0),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0)), List.of("Aldric"), List.of("Tanner"),
                Map.of()));
        when(settlerConfig.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));

        when(config.getArrivalEvery()).thenReturn(Duration.ofHours(4));
        when(config.getArrivalWait()).thenReturn(Duration.ofHours(2));
        when(config.getArrivalCounts()).thenReturn(Map.of(2, 1.0));
        when(config.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.RARE, 1.0),
                Map.of(CampProfessions.BUILDER, 1.0)));
        when(config.getArrivalPrices()).thenReturn(Map.of(SettlerRarity.RARE, 15_000L));
        when(config.getBoardSize()).thenReturn(4);
        when(config.getBoardRefresh()).thenReturn(Duration.ofHours(12));
        when(config.getReroll()).thenReturn(2_000L);
        when(config.getBoardOdds()).thenReturn(new SettlerOdds(Map.of(SettlerRarity.COMMON, 1.0),
                Map.of(SettlerOdds.NONE, 1.0)));
        when(config.getBoardPrices()).thenReturn(Map.of(SettlerRarity.COMMON, 5_000L));
        when(config.getMilestones()).thenReturn(new TreeMap<>(Map.of(
                5, new RecruitConfig.Milestone(CampProfessions.BUILDER, SettlerRarity.COMMON),
                10, new RecruitConfig.Milestone(RecruitConfig.ANY, SettlerRarity.LEGENDARY),
                25, new RecruitConfig.Milestone(CampProfessions.FARMER, SettlerRarity.RARE))));

        final ClanManager clanManager = mock(ClanManager.class);
        when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(clan));
        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(coins.take(any(), anyLong())).thenReturn(true);

        final ProfessionRegistry professions = new ProfessionRegistry();
        final TraitRegistry traits = new TraitRegistry();
        new CampProfessions(professions);
        new CampTraits(traits);
        recruitment = new CampRecruitment(store, settlers, new SettlerGenerator(professions, traits), settlerConfig,
                config, traits, mock(ConstructionService.class), mock(SiteInstances.class), clanManager, permissions,
                coins, new CampWideTraits(settlerConfig), now::get);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void theFirstBoatIsOnlyScheduled() {
        recruitment.settle(SITE, true);
        assertTrue(camp.getArrivals().isEmpty());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt());
    }

    @Test
    void aBoatBringsCandidatesWhoWaitAWhile() {
        recruitment.settle(SITE, true);
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(2, camp.getArrivals().size());
        final SettlerCandidate candidate = camp.getArrivals().getFirst();
        assertEquals(15_000, candidate.getPrice());
        assertEquals(now.get() + 2 * HOUR, candidate.getExpiresAt());
        assertEquals(CampProfessions.BUILDER, candidate.getSettler().getProfession());

        now.addAndGet(2 * HOUR);
        recruitment.settle(SITE, false);
        assertTrue(camp.getArrivals().isEmpty(), "they give up waiting");
    }

    @Test
    void aBoatRolledAheadLandsAsItWasSeen() {
        recruitment.settle(SITE, true);
        final List<SettlerCandidate> seen = recruitment.boat(null, new Random(1));
        camp.setNextBoat(new ArrayList<>(seen));
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(seen, camp.getArrivals());
        assertEquals(now.get() + 2 * HOUR, camp.getArrivals().getFirst().getExpiresAt());
        assertTrue(camp.getNextBoat().isEmpty());
    }

    @Test
    void aChosenProfessionIsGivenToEveryoneOnTheNextBoatOnly() {
        recruitment.settle(SITE, true);
        camp.setNextBoatProfession(CampProfessions.FARMER);
        now.addAndGet(4 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(2, camp.getArrivals().size());
        assertTrue(camp.getArrivals().stream()
                .allMatch(candidate -> CampProfessions.FARMER.equals(candidate.getSettler().getProfession())));
        assertNull(camp.getNextBoatProfession());
    }

    @Test
    void noBoatsComeWhileTheDockIsBroken() {
        recruitment.settle(SITE, false);
        now.addAndGet(9 * HOUR);
        recruitment.settle(SITE, false);

        assertTrue(camp.getArrivals().isEmpty());
        assertEquals(now.get() + 4 * HOUR, camp.getNextArrivalAt());
    }

    @Test
    void aClosedCampOnlyKeepsBoatsStillWaiting() {
        recruitment.settle(SITE, true);
        now.addAndGet(13 * HOUR);
        recruitment.settle(SITE, true);

        assertEquals(2, camp.getArrivals().size(), "only the boat due an hour ago is still there");
        assertTrue(camp.getNextArrivalAt() > now.get());
    }

    @Test
    void milestonesSendSettlersOnceWhoWaitForRoom() {
        when(clan.getLevel()).thenReturn(12L);
        recruitment.settle(SITE, false);
        recruitment.settle(SITE, false);

        assertEquals(2, camp.getArrivals().size());
        assertTrue(camp.getArrivals().stream().allMatch(candidate -> candidate.getPrice() == 0
                && candidate.getExpiresAt() == 0));
        assertEquals(SettlerRarity.LEGENDARY, camp.getArrivals().get(1).getSettler().getRarity());
        assertEquals(Set.of(5, 10), camp.getMilestones());
    }

    @Test
    void theBoardTurnsOverOnItsOwnOrForAFee() {
        final List<SettlerCandidate> first = List.copyOf(recruitment.board(SITE));
        assertEquals(4, first.size());
        assertEquals(first, recruitment.board(SITE));

        now.addAndGet(12 * HOUR);
        assertFalse(first.equals(recruitment.board(SITE)));

        final List<SettlerCandidate> second = List.copyOf(recruitment.board(SITE));
        assertNull(recruitment.reroll(player, SITE));
        assertFalse(second.equals(recruitment.board(SITE)));
        verify(coins).take(player, 2_000);
    }

    @Test
    void hiringChargesJoinsAndRemovesTheCandidate() {
        final SettlerCandidate candidate = recruitment.board(SITE).getFirst();
        final UUID id = candidate.getSettler().getId();

        assertTrue(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins).take(player, 5_000);
        assertTrue(recruitment.find(SITE, id).isEmpty());
    }

    @Test
    void hiringNeedsRoomPermissionAndCoins() {
        final UUID id = recruitment.board(SITE).getFirst().getSettler().getId();
        when(settlers.populationCap(SITE)).thenReturn(0);
        assertFalse(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins, never()).take(any(), anyLong());

        when(settlers.populationCap(SITE)).thenReturn(6);
        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        assertFalse(recruitment.hire(player, SITE, id).isSuccess());

        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(settlers.grant(any(), any())).thenReturn(SettlerResult.refused("core.settler.not_loaded"));
        assertFalse(recruitment.hire(player, SITE, id).isSuccess());
        verify(coins).give(player, 5_000);
        assertTrue(recruitment.find(SITE, id).isPresent(), "a hire that fell through leaves them waiting");
    }

    @Test
    void recruitersAndHagglersHelp() {
        final Settler recruiter = new Settler();
        recruiter.setId(UUID.randomUUID());
        recruiter.setRarity(SettlerRarity.LEGENDARY);
        recruiter.setTraits(new ArrayList<>(List.of(CampTraits.RECRUITER, CampTraits.HAGGLER)));
        roster.getSettlers().add(recruiter);

        assertEquals((long) (4 * HOUR * 0.7), recruitment.interval(SITE));
        assertEquals(4_000, recruitment.price(SITE, new SettlerCandidate(recruiter, 5_000, 0)));
    }
}
