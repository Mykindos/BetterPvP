package me.mykindos.betterpvp.core.world.settler.wage;

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
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class PayrollTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(HOUR);
    private final List<Event> events = new ArrayList<>();
    private final Site site = new Site();

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
        payroll = new Payroll(settlers, mock(SiteInstances.class), now::get);
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

    private void settleAfter(long millis) {
        now.addAndGet(millis);
        payroll.settle(CAMP);
    }

    @Test
    void theFirstSettlementOnlyStartsTheClock() {
        settler("builder", SettlerRarity.COMMON);
        site.fund = 1_000;
        payroll.settle(CAMP);

        assertEquals(1_000, site.fund);
        assertEquals(HOUR, site.roster.getPayrollAt());
    }

    @Test
    void paidSettlersCostTheirRateOverRealTime() {
        settler("builder", SettlerRarity.COMMON);
        settler("farmer", SettlerRarity.LEGENDARY);
        site.fund = 1_000;
        payroll.settle(CAMP);

        settleAfter(HOUR);
        assertEquals(700, site.fund, "only Builders are paid");
    }

    @Test
    void partCoinsCarryOverRatherThanBeingLost() {
        settler("builder", SettlerRarity.LEGENDARY);
        site.fund = 10_000;
        payroll.settle(CAMP);

        for (int minute = 0; minute < 60; minute++) {
            settleAfter(60_000);
        }
        assertEquals(10_000 - 2_500, site.fund);
    }

    @Test
    void anEmptyFundStartsAStrikeWhenTheMoneyRanOut() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        site.fund = 150;
        payroll.settle(CAMP);
        final long start = now.get();

        settleAfter(HOUR);
        assertEquals(0, site.fund);
        assertEquals(SettlerState.STRIKING, builder.getState());
        assertEquals(start + HOUR / 2, builder.getStateSince());
        assertTrue(events.stream().anyMatch(event -> event instanceof SettlerStrikeEvent strike && strike.isStriking()));

        settleAfter(HOUR);
        assertEquals(SettlerState.STRIKING, builder.getState(), "strikers are not paid and stay out");
    }

    @Test
    void payingTheFundSendsStrikersBackToWhatTheyWereDoing() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        builder.setAssignment("job");
        builder.setState(SettlerState.WORKING);
        payroll.settle(CAMP);
        settleAfter(HOUR);
        assertEquals(SettlerState.STRIKING, builder.getState());

        site.fund = 1_000;
        settleAfter(1_000);
        assertEquals(SettlerState.WORKING, builder.getState());
        assertTrue(events.stream().anyMatch(event -> event instanceof SettlerStrikeEvent strike && !strike.isStriking()));
    }

    @Test
    void aStrikerStillUnpaidAfterTheLimitLeaves() {
        final Settler builder = settler("builder", SettlerRarity.COMMON);
        payroll.settle(CAMP);

        settleAfter(73 * HOUR);
        assertTrue(site.roster.find(builder.getId()).isEmpty(), "a camp closed for three days is caught up at once");
        assertTrue(events.stream().anyMatch(event -> event instanceof SettlerLeftEvent left
                && left.getReason() == SettlerLeaveReason.UNPAID));
    }

    @Test
    void theIdleAndWorkingModelPaysMoreForWork() {
        site.model = new IdleWorkingWageModel(Map.of("builder", Map.of(SettlerRarity.COMMON, 150.0)),
                Map.of("builder", Map.of(SettlerRarity.COMMON, 450.0)));
        final Settler idle = settler("builder", SettlerRarity.COMMON);
        final Settler working = settler("builder", SettlerRarity.COMMON);
        working.setState(SettlerState.WORKING);

        assertEquals(150, payroll.hourly(CAMP, idle));
        assertEquals(450, payroll.hourly(CAMP, working));
        assertEquals(600, payroll.hourly(CAMP));
    }

    private static final class Site implements SettlerSite, CoinAccount {

        private final Roster roster = new Roster();
        private long fund;
        private WageModel model = new FixedWageModel(Map.of("builder", Map.of(
                SettlerRarity.COMMON, 300.0, SettlerRarity.LEGENDARY, 2_500.0)));

        @Override
        public long balance(@NotNull SiteKey site) {
            return fund;
        }

        @Override
        public void withdraw(@NotNull SiteKey site, long amount) {
            fund -= amount;
        }

        @Override
        public @NotNull Optional<Roster> roster(@NotNull SiteKey site) {
            return Optional.of(roster);
        }

        @Override
        public void changed(@NotNull SiteKey site) {
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
            return Optional.of(model);
        }

        @Override
        public @NotNull Optional<CoinAccount> wageFund(@NotNull SiteKey site) {
            return Optional.of(this);
        }
    }
}
