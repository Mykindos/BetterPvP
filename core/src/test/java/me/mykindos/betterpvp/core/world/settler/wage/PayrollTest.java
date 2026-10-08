package me.mykindos.betterpvp.core.world.settler.wage;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerLeftEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);
    private static final SiteKey CLOSED = SiteKey.of("camp", 8);
    private static final long HOUR = 3_600_000L;
    private static final long MINUTE = 60_000L;
    private static final WageModel PRD_FIXED = new FixedWageModel(Map.of("builder", Map.of(
            SettlerRarity.COMMON, 300.0, SettlerRarity.UNCOMMON, 600.0,
            SettlerRarity.RARE, 1_200.0, SettlerRarity.LEGENDARY, 2_500.0)));
    private static final WageModel PRD_IDLE_WORKING = new IdleWorkingWageModel(
            Map.of("builder", Map.of(SettlerRarity.COMMON, 150.0)),
            Map.of("builder", Map.of(SettlerRarity.COMMON, 450.0)));

    private final AtomicLong now = new AtomicLong(HOUR);
    private final List<Event> events = new ArrayList<>();
    private final Site site = new Site();
    private final SiteInstances instances = mock(SiteInstances.class);

    private MockedStatic<Bukkit> bukkit;
    private Payroll payroll;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        final ProfessionRegistry professions = new ProfessionRegistry();
        professions.register(Profession.construction("builder", "builder", List.of()));
        professions.register(Profession.workplace("farmer", "farmer", "farm"));
        final SettlerService settlers = new SettlerService(professions);
        settlers.register("camp", site);
        payroll = new Payroll(settlers, instances, now::get);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Settler settler(String profession, SettlerRarity rarity) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Hal Two-Coats");
        settler.setProfession(profession);
        settler.setRarity(rarity);
        site.roster.getSettlers().add(settler);
        return settler;
    }

    private Settler striker(String assignment) {
        final Settler settler = settler("builder", SettlerRarity.COMMON);
        settler.setAssignment(assignment);
        settler.changeState(SettlerState.STRIKING, now.get());
        return settler;
    }

    private void settleAfter(long millis) {
        now.addAndGet(millis);
        payroll.settle(CAMP);
    }

    private List<SettlerStrikeEvent> strikes(boolean striking) {
        return events.stream()
                .filter(event -> event instanceof SettlerStrikeEvent strike && strike.isStriking() == striking)
                .map(SettlerStrikeEvent.class::cast)
                .toList();
    }

    @Test
    void ac1_aSettlerCostsItsModelsRateTimesItsMultiplier() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        final Settler greedy = settler("builder", SettlerRarity.COMMON);
        final Settler farmer = settler("farmer", SettlerRarity.LEGENDARY);
        final Settler wanderer = settler(null, SettlerRarity.LEGENDARY);
        site.multipliers.put(greedy.getId(), 1.25);

        assertEquals(300, payroll.hourly(CAMP, builder));
        assertEquals(375, payroll.hourly(CAMP, greedy));
        assertEquals(0, payroll.hourly(CAMP, farmer));
        assertEquals(0, payroll.hourly(CAMP, wanderer));

        site.model = null;
        assertEquals(0, payroll.hourly(CAMP, builder));
    }

    @Test
    void ac2_theFixedModelPaysOneRatePerProfessionAndRarity() {
        final Settler builder = new Settler();
        builder.setProfession("builder");
        final Map<SettlerRarity, Double> prd = Map.of(SettlerRarity.COMMON, 300.0, SettlerRarity.UNCOMMON, 600.0,
                SettlerRarity.RARE, 1_200.0, SettlerRarity.LEGENDARY, 2_500.0);
        prd.forEach((rarity, rate) -> {
            builder.setRarity(rarity);
            assertEquals(rate, PRD_FIXED.perHour(builder, false));
            assertEquals(rate, PRD_FIXED.perHour(builder, true));
        });

        final WageModel commonOnly = new FixedWageModel(Map.of("builder", Map.of(SettlerRarity.COMMON, 300.0)));
        builder.setRarity(SettlerRarity.RARE);
        assertEquals(0, commonOnly.perHour(builder, true));
        builder.setProfession("farmer");
        builder.setRarity(SettlerRarity.COMMON);
        assertEquals(0, PRD_FIXED.perHour(builder, true));
    }

    @Test
    void ac3_theIdleAndWorkingModelPaysTheWorkingRateWithAnAssignment() {
        site.model = PRD_IDLE_WORKING;
        final Settler idle = settler("builder", SettlerRarity.COMMON);
        final Settler working = settler("builder", SettlerRarity.COMMON);
        working.setAssignment("job");
        working.setState(SettlerState.WORKING);
        final Settler assignedStriker = striker("job");
        final Settler idleStriker = striker(null);

        assertEquals(150, payroll.hourly(CAMP, idle));
        assertEquals(450, payroll.hourly(CAMP, working));
        assertEquals(450, payroll.hourly(CAMP, assignedStriker));
        assertEquals(150, payroll.hourly(CAMP, idleStriker));
    }

    @Test
    void ac4_theSitesHourlyCostAddsUpEveryoneStrikersIncluded() {
        settler("builder", SettlerRarity.COMMON);
        settler("builder", SettlerRarity.LEGENDARY).changeState(SettlerState.STRIKING, now.get());
        settler("farmer", SettlerRarity.RARE);

        assertEquals(2_800, payroll.hourly(CAMP));
    }

    @Test
    void ac5_theFirstSettlementOnlyStartsTheClock() {
        settler("builder", SettlerRarity.COMMON);
        site.fund.balance = 1_000;
        payroll.settle(CAMP);

        assertEquals(1_000, site.fund.balance);
        assertTrue(site.fund.withdrawals.isEmpty());
        assertEquals(HOUR, site.roster.getPayrollAt());
    }

    @Test
    void ac5_noFundOrModelMovesTheClockWithoutCharging() {
        settler("builder", SettlerRarity.COMMON);
        final Fund fund = site.fund;
        fund.balance = 1_000;
        site.fund = null;
        payroll.settle(CAMP);
        settleAfter(10 * HOUR);
        assertEquals(now.get(), site.roster.getPayrollAt());

        site.fund = fund;
        settleAfter(MINUTE);
        assertEquals(1_000 - 5, fund.balance, "only the minute since the fund appeared is charged");

        site.model = null;
        settleAfter(10 * HOUR);
        assertEquals(now.get(), site.roster.getPayrollAt());
        site.model = PRD_FIXED;
        settleAfter(MINUTE);
        assertEquals(1_000 - 10, fund.balance, "only the minute since the model appeared is charged");
    }

    @Test
    void ac6_paidSettlersAreChargedForRealTimeAndPartCoinsCarry() {
        settler("builder", SettlerRarity.LEGENDARY);
        settler("farmer", SettlerRarity.LEGENDARY);
        site.fund.balance = 10_000;
        payroll.settle(CAMP);

        for (int minute = 0; minute < 60; minute++) {
            settleAfter(MINUTE);
        }
        assertEquals(10_000 - 2_500, site.fund.balance);
        assertTrue(site.fund.withdrawals.stream().allMatch(amount -> amount == 41 || amount == 42));
    }

    @Test
    void ac7_timeWhileTheWorldWasClosedIsChargedInFull() {
        settler("builder", SettlerRarity.COMMON);
        site.fund.balance = 10_000;
        payroll.settle(CAMP);

        settleAfter(24 * HOUR);
        assertEquals(10_000 - 24 * 300, site.fund.balance);
    }

    @Test
    void ac8_wagesNeverTakeMoreThanTheBalance() {
        settler("builder", SettlerRarity.COMMON);
        settler("builder", SettlerRarity.RARE);
        site.fund.balance = 150;
        payroll.settle(CAMP);

        settleAfter(HOUR);
        settleAfter(HOUR);
        assertFalse(site.fund.overdrawn);
        assertEquals(0, site.fund.balance);
    }

    @Test
    void ac9_aSettlementOwingLessThanACoinLeavesTheFundAlone() {
        settler("builder", SettlerRarity.COMMON);
        site.fund.balance = 1_000;
        payroll.settle(CAMP);

        settleAfter(1_000);
        assertTrue(site.fund.withdrawals.isEmpty(), "a twelfth of a coin owed should not touch the fund");
    }

    @Test
    void ac9_aFundRunningOutAtZeroIsNotTouched() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);

        settleAfter(MINUTE);
        assertEquals(SettlerState.STRIKING, builder.getState());
        assertTrue(site.fund.withdrawals.isEmpty(), "an empty fund should not be asked for 0 coins");
    }

    @Test
    void ac10_onlySitesWithALoadedWorldAreSettledAndChangesAreWrittenDown() {
        settler("builder", SettlerRarity.COMMON);
        site.rosters.put(CLOSED, new Roster());
        when(instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY),
                new SiteInstance(UUID.randomUUID(), CLOSED, "camp_8", SiteInstance.State.DORMANT)));
        bukkit.when(() -> Bukkit.getWorld("camp_7")).thenReturn(mock(World.class));

        payroll.tick();
        assertEquals(HOUR, site.roster.getPayrollAt());
        assertEquals(0, site.rosters.get(CLOSED).getPayrollAt());
        assertTrue(site.changed.contains(CAMP));
        assertFalse(site.changed.contains(CLOSED));

        site.changed.clear();
        site.fund.balance = 1_000;
        settleAfter(HOUR);
        assertTrue(site.changed.contains(CAMP));
    }

    @Test
    void ac11_wageAndMoraleClocksSurviveARoundTrip() throws Exception {
        final Settler striker = striker("job");
        striker.setStateSince(12_345);
        final Settler unhappy = settler("farmer", SettlerRarity.RARE);
        unhappy.setUnhappySince(67_890);
        site.roster.setPayrollAt(99_000);
        site.roster.setPayrollCarry(0.4);

        final ObjectMapper mapper = new ObjectMapper();
        final Roster read = mapper.readValue(mapper.writeValueAsString(site.roster), Roster.class);

        assertEquals(99_000, read.getPayrollAt());
        assertEquals(0.4, read.getPayrollCarry(), 1e-9);
        assertEquals(12_345, read.find(striker.getId()).orElseThrow().getStateSince());
        assertEquals(SettlerState.STRIKING, read.find(striker.getId()).orElseThrow().getState());
        assertEquals(67_890, read.find(unhappy.getId()).orElseThrow().getUnhappySince());
    }

    @Test
    void ac12_anEmptyFundStrikesEveryPaidSettlerFromWhenTheMoneyRanOut() {
        final Settler first = settler("builder", SettlerRarity.COMMON);
        final Settler second = settler("builder", SettlerRarity.COMMON);
        final Settler farmer = settler("farmer", SettlerRarity.COMMON);
        final Settler already = striker(null);
        already.setStateSince(5);
        site.fund.balance = 300;
        payroll.settle(CAMP);
        final long start = now.get();

        settleAfter(HOUR);
        assertEquals(0, site.fund.balance);
        assertEquals(SettlerState.STRIKING, first.getState());
        assertEquals(SettlerState.STRIKING, second.getState());
        assertEquals(start + HOUR / 2, first.getStateSince());
        assertEquals(start + HOUR / 2, second.getStateSince());
        assertEquals(SettlerState.IDLE, farmer.getState());
        assertEquals(5, already.getStateSince());

        final List<SettlerStrikeEvent> started = strikes(true);
        assertEquals(1, started.size());
        assertEquals(List.of(first, second), started.getFirst().getSettlers());
    }

    @Test
    void ac13_strikersReturnOnceTheFundCanPayAMinute() {
        final Settler assigned = settler("builder", SettlerRarity.COMMON);
        assigned.setAssignment("job");
        assigned.setState(SettlerState.WORKING);
        final Settler unassigned = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);
        settleAfter(HOUR);
        assertEquals(SettlerState.STRIKING, assigned.getState());
        assertTrue(strikes(false).isEmpty(), "no one returns in the settlement that started the strike");

        site.fund.balance = 9;
        settleAfter(HOUR);
        assertEquals(9, site.fund.balance, "strikers are not paid");
        assertEquals(SettlerState.STRIKING, assigned.getState(), "9 coins is less than a minute of 600 an hour");

        site.fund.balance = 10;
        settleAfter(1_000);
        assertEquals(SettlerState.WORKING, assigned.getState());
        assertEquals(SettlerState.IDLE, unassigned.getState());
        final List<SettlerStrikeEvent> ended = strikes(false);
        assertEquals(1, ended.size());
        assertEquals(2, ended.getFirst().getSettlers().size());
    }

    @Test
    void ac14_withNoFundStrikersGoBackToWork() {
        final Settler striker = striker("job");
        site.fund = null;
        payroll.settle(CAMP);
        settleAfter(MINUTE);

        assertEquals(SettlerState.WORKING, striker.getState(), "a site with no fund owes nobody");
        assertReturnedOnce(striker);
    }

    @Test
    void ac14_withNoWageModelStrikersGoBackToWork() {
        final Settler striker = striker(null);
        site.model = null;
        payroll.settle(CAMP);
        settleAfter(MINUTE);

        assertEquals(SettlerState.IDLE, striker.getState(), "a site with no wage model owes nobody");
        assertReturnedOnce(striker);
    }

    @Test
    void ac14_whenEveryRateIsZeroStrikersGoBackToWork() {
        final Settler striker = striker(null);
        site.model = new FixedWageModel(Map.of());
        payroll.settle(CAMP);
        settleAfter(MINUTE);

        assertEquals(SettlerState.IDLE, striker.getState(), "a settler that costs nothing is owed nothing");
        assertReturnedOnce(striker);
    }

    private void assertReturnedOnce(Settler striker) {
        final List<SettlerStrikeEvent> ended = strikes(false);
        assertEquals(1, ended.size());
        assertEquals(List.of(striker), ended.getFirst().getSettlers());
    }

    @Test
    void ac15_aStrikerPastTheLimitLeavesAsUnpaid() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);
        settleAfter(MINUTE);
        assertEquals(SettlerState.STRIKING, builder.getState());

        settleAfter(71 * HOUR);
        assertTrue(site.roster.find(builder.getId()).isPresent());
        settleAfter(HOUR);
        assertTrue(site.roster.find(builder.getId()).isEmpty());
        assertTrue(events.stream().anyMatch(event -> event instanceof SettlerLeftEvent left
                && left.getSettler() == builder && left.getReason() == SettlerLeaveReason.UNPAID));
    }

    @Test
    void ac15_theWholeLimitPassingWhileClosedStillCounts() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);

        settleAfter(73 * HOUR);
        assertTrue(site.roster.find(builder.getId()).isEmpty());
    }

    @Test
    void ac15_theSitesStrikeLimitIsUsed() {
        site.strikeLimit = Duration.ofHours(1);
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);

        settleAfter(HOUR);
        assertTrue(site.roster.find(builder.getId()).isEmpty());
    }

    @Test
    void ac15_aStrikerPaidBackToWorkStays() {
        payroll.settle(CAMP);
        final Settler striker = striker(null);
        striker.setStateSince(now.get() - 100 * HOUR);
        site.fund.balance = 1_000;

        settleAfter(MINUTE);
        assertTrue(site.roster.find(striker.getId()).isPresent());
        assertEquals(SettlerState.IDLE, striker.getState());
    }

    private static final class Fund implements CoinAccount {

        private long balance;
        private boolean overdrawn;
        private final List<Long> withdrawals = new ArrayList<>();

        @Override
        public long balance(@NotNull SiteKey site) {
            return balance;
        }

        @Override
        public void withdraw(@NotNull SiteKey site, long amount) {
            overdrawn |= amount > balance;
            withdrawals.add(amount);
            balance -= amount;
        }
    }

    private static final class Site implements SettlerSite {

        private final Roster roster = new Roster();
        private final Map<SiteKey, Roster> rosters = new HashMap<>(Map.of(CAMP, roster));
        private final Map<UUID, Double> multipliers = new HashMap<>();
        private final List<SiteKey> changed = new ArrayList<>();
        private Fund fund = new Fund();
        private WageModel model = PRD_FIXED;
        private Duration strikeLimit;

        @Override
        public @NotNull Optional<Roster> roster(@NotNull SiteKey site) {
            return Optional.ofNullable(rosters.get(site));
        }

        @Override
        public void changed(@NotNull SiteKey site) {
            changed.add(site);
        }

        @Override
        public int populationCap(@NotNull SiteKey site) {
            return 20;
        }

        @Override
        public @NotNull OptionalInt workingCap(@NotNull SiteKey site, @NotNull String profession) {
            return OptionalInt.empty();
        }

        @Override
        public boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull SettlerAction action) {
            return true;
        }

        @Override
        public @NotNull SettlerLook look(@NotNull SiteKey site, @NotNull Settler settler) {
            return new SettlerLook("model", null, "idle", "walk", "work", 1);
        }

        @Override
        public @NotNull Optional<WageModel> wageModel(@NotNull SiteKey site) {
            return Optional.ofNullable(model);
        }

        @Override
        public @NotNull Optional<CoinAccount> wageFund(@NotNull SiteKey site) {
            return Optional.ofNullable(fund);
        }

        @Override
        public double wageMultiplier(@NotNull SiteKey site, @NotNull Settler settler) {
            return multipliers.getOrDefault(settler.getId(), 1.0);
        }

        @Override
        public @NotNull Duration strikeLimit(@NotNull SiteKey site) {
            return strikeLimit == null ? SettlerSite.super.strikeLimit(site) : strikeLimit;
        }
    }
}
