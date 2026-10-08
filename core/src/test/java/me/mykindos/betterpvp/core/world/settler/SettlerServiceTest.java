package me.mykindos.betterpvp.core.world.settler;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class SettlerServiceTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);
    private static final long DAY = 24L * 60 * 60 * 1000;

    private final AtomicLong now = new AtomicLong(1_000);
    private final List<Event> events = new ArrayList<>();
    private final FakeSite site = new FakeSite();
    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final Player player = mock(Player.class);

    private MockedStatic<Bukkit> bukkit;
    private SettlerService service;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        professions.register(Profession.construction("builder", "builder", List.of()));
        professions.register(Profession.workplace("farmer", "farmer", "farm"));
        service = new SettlerService(professions, now::get);
        service.register("camp", site);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private static Settler settler(String profession) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Tamsin Reed");
        settler.setProfession(profession);
        return settler;
    }

    private Settler granted(String profession) {
        return service.grant(CAMP, settler(profession)).getSettler();
    }

    private static String reason(SettlerResult result) {
        assertFalse(result.isSuccess(), "expected a refusal");
        return ((TranslatableComponent) result.getReason()).key();
    }

    private <T extends Event> List<T> fired(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    void ac1_settlersOnlyLiveWhereTheSiteIsRegisteredAndItsRosterLoaded() {
        final Settler builder = granted("builder");
        final SiteKey elsewhere = SiteKey.of("elsewhere", 1);

        assertEquals("core.settler.not_loaded", reason(service.grant(elsewhere, settler("builder"))));
        assertEquals("core.settler.not_loaded", reason(service.assign(elsewhere, builder.getId(), "job-a")));
        assertEquals("core.settler.not_loaded", reason(service.unassign(elsewhere, builder.getId())));
        assertEquals("core.settler.not_loaded", reason(service.dismiss(elsewhere, builder.getId())));
        assertEquals("core.settler.not_loaded",
                reason(service.remove(elsewhere, builder.getId(), SettlerLeaveReason.UNHAPPY)));
        assertTrue(service.site(elsewhere).isEmpty());
        assertTrue(service.roster(elsewhere).isEmpty());
        assertEquals(0, service.populationCap(elsewhere));
        assertTrue(service.workingCap(elsewhere, "builder").isEmpty());

        site.loaded = false;
        assertEquals("core.settler.not_loaded", reason(service.grant(CAMP, settler("builder"))));
        assertEquals("core.settler.not_loaded", reason(service.assign(CAMP, builder.getId(), "job-a")));
        assertEquals("core.settler.not_loaded", reason(service.unassign(CAMP, builder.getId())));
        assertEquals("core.settler.not_loaded", reason(service.dismiss(CAMP, builder.getId())));
        assertEquals("core.settler.not_loaded",
                reason(service.remove(CAMP, builder.getId(), SettlerLeaveReason.UNPAID)));
        assertTrue(service.roster(CAMP).isEmpty());
    }

    @Test
    void ac2_everyChangeIsWrittenDownAndARefusalWritesNothing() {
        final Settler builder = granted("builder");
        assertEquals(1, site.changes);
        service.assign(CAMP, builder.getId(), "job-a");
        assertEquals(2, site.changes);
        service.unassign(CAMP, builder.getId());
        assertEquals(3, site.changes);
        service.dismiss(CAMP, builder.getId());
        assertEquals(4, site.changes);

        final Settler farmer = granted("farmer");
        final Settler striker = granted("builder");
        striker.changeState(SettlerState.STRIKING, 5);
        final int before = site.changes;
        site.population = 2;

        assertFalse(service.grant(CAMP, settler(null)).isSuccess());
        assertFalse(service.assign(CAMP, farmer.getId(), "job-a").isSuccess());
        assertFalse(service.assign(CAMP, striker.getId(), "job-a").isSuccess());
        assertFalse(service.dismiss(CAMP, UUID.randomUUID()).isSuccess());
        assertFalse(service.unassign(CAMP, UUID.randomUUID()).isSuccess());

        assertEquals(before, site.changes);
        assertEquals(2, site.roster.size());
        assertNull(farmer.getAssignment());
        assertEquals(SettlerState.IDLE, farmer.getState());
        assertNull(striker.getAssignment());
    }

    @Test
    void ac3_aRosterSurvivesARoundTrip() throws Exception {
        final Settler builder = granted("builder");
        builder.setSpecialty("mason");
        builder.setTraits(new ArrayList<>(List.of("foreman", "greedy")));
        builder.setHistory("history.dock");
        builder.setHistoryArgs(new ArrayList<>(List.of("Saltmere")));
        builder.setRarity(SettlerRarity.RARE);
        builder.setMorale(-35);
        builder.setJobsFinished(4);
        service.assign(CAMP, builder.getId(), "job-a");
        final Settler leaver = granted(null);
        now.set(5_000);
        service.remove(CAMP, leaver.getId(), SettlerLeaveReason.UNPAID);

        final ObjectMapper mapper = new ObjectMapper();
        final Roster read = mapper.readValue(mapper.writeValueAsString(site.roster), Roster.class);

        final Settler copy = read.find(builder.getId()).orElseThrow();
        assertEquals(builder, copy);
        assertEquals("Tamsin Reed", copy.getName());
        assertEquals(List.of("Saltmere"), copy.getHistoryArgs());
        assertEquals(SettlerState.WORKING, copy.getState());
        assertEquals("job-a", copy.getAssignment());
        assertEquals(1_000, copy.getJoinedAt());
        assertEquals(1, read.working("builder"));
        assertEquals(site.roster.getSettlers(), read.getSettlers());
        assertEquals(site.roster.getDepartures(), read.getDepartures());
        assertEquals(new SettlerDeparture("Tamsin Reed", SettlerRarity.COMMON, SettlerLeaveReason.UNPAID, 5_000),
                read.getDepartures().getFirst());
    }

    @Test
    void ac4_grantingIsRefusedWhenFullOrAlreadyThere() {
        site.population = 2;

        final Settler first = settler("builder");
        assertTrue(service.grant(CAMP, first).isSuccess());
        assertFalse(service.grant(CAMP, first).isSuccess(), "a settler already on the roster is refused");
        assertEquals(1, site.roster.size());

        assertTrue(service.grant(CAMP, settler(null)).isSuccess());
        final SettlerResult full = service.grant(CAMP, settler("farmer"));
        assertEquals("core.settler.population_full", reason(full));
        assertEquals(Component.text(2), ((TranslatableComponent) full.getReason()).arguments().getFirst().asComponent());
        assertEquals(2, site.roster.size());
    }

    @Test
    void ac5_aGrantedSettlerJoinsIdleAndUnassigned() {
        site.caps.put("builder", 0);
        now.set(3_000);
        final Settler incoming = settler("builder");
        incoming.setAssignment("job-a");
        incoming.setState(SettlerState.STRIKING);

        final SettlerResult result = service.grant(CAMP, incoming);

        assertTrue(result.isSuccess(), "a professional joins even when its working cap is full");
        assertSame(incoming, site.roster.getSettlers().getFirst());
        assertEquals(SettlerState.IDLE, incoming.getState());
        assertNull(incoming.getAssignment());
        assertEquals(3_000, incoming.getJoinedAt());
        final List<SettlerJoinedEvent> joined = fired(SettlerJoinedEvent.class);
        assertEquals(1, joined.size());
        assertSame(incoming, joined.getFirst().getSettler());
        assertEquals(CAMP, joined.getFirst().getSite());
    }

    @Test
    void ac6_settlersOnlyWorkWhereTheirProfessionDoes() {
        final Settler builder = granted("builder");
        final Settler farmer = granted("farmer");
        final Settler wanderer = granted(null);
        final Settler unknown = granted("wizard");

        assertTrue(service.assign(CAMP, builder.getId(), "job-a").isSuccess());
        assertEquals("core.settler.wrong_workplace", reason(service.assign(CAMP, farmer.getId(), "job-a")));
        assertTrue(service.assign(CAMP, farmer.getId(), "farm").isSuccess());
        assertEquals("core.settler.no_profession", reason(service.assign(CAMP, wanderer.getId(), "farm")));
        assertEquals("core.settler.no_profession", reason(service.assign(CAMP, unknown.getId(), "job-a")));
    }

    @Test
    void ac7_assigningFiresWithWhereItWasAndRepeatingItDoesNothing() {
        final Settler builder = granted("builder");
        now.set(2_000);

        assertTrue(service.assign(CAMP, builder.getId(), "job-a").isSuccess());
        assertEquals(SettlerState.WORKING, builder.getState());
        assertEquals("job-a", builder.getAssignment());
        assertEquals(2_000, builder.getStateSince());
        assertTrue(service.assign(CAMP, builder.getId(), "job-b").isSuccess());

        final int changes = site.changes;
        assertTrue(service.assign(CAMP, builder.getId(), "job-b").isSuccess());
        assertEquals(changes, site.changes);

        final List<SettlerAssignedEvent> assigned = fired(SettlerAssignedEvent.class);
        assertEquals(2, assigned.size());
        assertNull(assigned.get(0).getPrevious());
        assertEquals("job-a", assigned.get(1).getPrevious());
        assertEquals("job-b", assigned.get(1).getSettler().getAssignment());
    }

    @Test
    void ac8_theWorkingCapLimitsHowManyWorkNotHowManyLive() {
        site.caps.put("builder", 1);
        final Settler first = granted("builder");
        final Settler second = granted("builder");

        assertTrue(service.assign(CAMP, first.getId(), "job-a").isSuccess());
        assertEquals("core.settler.working_full", reason(service.assign(CAMP, second.getId(), "job-a")));
        assertTrue(service.assign(CAMP, first.getId(), "job-b").isSuccess(), "a moved settler is not counted twice");
        assertEquals(SettlerState.WORKING, first.getState());
        assertEquals(SettlerState.IDLE, second.getState());

        assertTrue(service.unassign(CAMP, first.getId()).isSuccess());
        assertTrue(service.assign(CAMP, second.getId(), "job-a").isSuccess());

        site.population = 5;
        final List<Settler> farmers = List.of(granted("farmer"), granted("farmer"), granted("farmer"));
        farmers.forEach(farmer -> assertTrue(service.assign(CAMP, farmer.getId(), "farm").isSuccess(),
                "with no working cap only the population cap applies"));
    }

    @Test
    void ac9_strikersWillNotBeAssigned() {
        final Settler builder = granted("builder");
        builder.changeState(SettlerState.STRIKING, 5);

        assertEquals("core.settler.will_not_work", reason(service.assign(CAMP, builder.getId(), "job-a")));
        assertNull(builder.getAssignment());
        assertEquals(SettlerState.STRIKING, builder.getState());
    }

    @Test
    void ac10_aSettlerOnARunningJobStaysUntilItEnds() {
        final Settler builder = granted("builder");
        service.assign(CAMP, builder.getId(), "job-a");
        site.running.add(builder.getId());
        final int changes = site.changes;

        assertEquals("core.settler.job_running", reason(service.assign(CAMP, builder.getId(), "job-b")));
        assertEquals("core.settler.job_running", reason(service.unassign(CAMP, builder.getId())));
        assertEquals("core.settler.job_running", reason(service.dismiss(CAMP, builder.getId())));
        assertEquals("job-a", builder.getAssignment());
        assertEquals(SettlerState.WORKING, builder.getState());
        assertTrue(site.roster.find(builder.getId()).isPresent());
        assertEquals(changes, site.changes);
        assertTrue(site.runningAsked > 0, "the service asks the site whether the job is running");

        events.clear();
        assertTrue(service.assign(CAMP, builder.getId(), "job-a").isSuccess(),
                "assigning it to the job it is already on is not refused");
        assertEquals("job-a", builder.getAssignment());
        assertEquals(changes, site.changes);
        assertTrue(fired(SettlerAssignedEvent.class).isEmpty());

        site.running.clear();
        assertTrue(service.assign(CAMP, builder.getId(), "job-b").isSuccess());
        assertTrue(service.unassign(CAMP, builder.getId()).isSuccess());
        assertTrue(service.dismiss(CAMP, builder.getId()).isSuccess());
    }

    @Test
    void ac11_unassigningLeavesItIdleOrStillStriking() {
        final Settler worker = granted("builder");
        final Settler striker = granted("builder");
        final Settler idle = granted("builder");
        service.assign(CAMP, worker.getId(), "job-a");
        service.assign(CAMP, striker.getId(), "job-b");
        striker.changeState(SettlerState.STRIKING, 5);
        events.clear();

        assertTrue(service.unassign(CAMP, worker.getId()).isSuccess());
        assertTrue(service.unassign(CAMP, striker.getId()).isSuccess());
        assertEquals(SettlerState.IDLE, worker.getState());
        assertNull(worker.getAssignment());
        assertEquals(SettlerState.STRIKING, striker.getState());
        assertNull(striker.getAssignment());
        final List<SettlerAssignedEvent> assigned = fired(SettlerAssignedEvent.class);
        assertEquals(List.of("job-a", "job-b"), assigned.stream().map(SettlerAssignedEvent::getPrevious).toList());

        final int changes = site.changes;
        assertTrue(service.unassign(CAMP, idle.getId()).isSuccess());
        assertEquals(changes, site.changes);
        assertEquals(2, fired(SettlerAssignedEvent.class).size());
    }

    @Test
    void ac12_playerActionsNeedTheSitesPermission() {
        final Settler builder = granted("builder");
        site.allowed = false;

        assertEquals("core.settler.not_allowed", reason(service.assign(player, CAMP, builder.getId(), "job-a")));
        assertNull(builder.getAssignment());
        service.assign(CAMP, builder.getId(), "job-a");
        assertEquals("core.settler.not_allowed", reason(service.unassign(player, CAMP, builder.getId())));
        assertEquals("job-a", builder.getAssignment());
        assertEquals("core.settler.not_allowed", reason(service.dismiss(player, CAMP, builder.getId())));
        assertTrue(site.roster.find(builder.getId()).isPresent());
        assertEquals(Set.of(SettlerAction.ASSIGN, SettlerAction.DISMISS), new HashSet<>(site.asked));

        site.allowed = true;
        assertTrue(service.unassign(player, CAMP, builder.getId()).isSuccess());
        assertTrue(service.assign(player, CAMP, builder.getId(), "job-b").isSuccess());
        assertTrue(service.dismiss(player, CAMP, builder.getId()).isSuccess());
    }

    @Test
    void ac13_leavingTakesItOffTheRosterForGood() {
        final Settler dismissed = granted("builder");
        final Settler unhappy = granted("farmer");
        final Settler unpaid = granted(null);

        assertTrue(service.dismiss(CAMP, dismissed.getId()).isSuccess());
        assertTrue(service.remove(CAMP, unhappy.getId(), SettlerLeaveReason.UNHAPPY).isSuccess());
        assertTrue(service.remove(CAMP, unpaid.getId(), SettlerLeaveReason.UNPAID).isSuccess());

        assertEquals(0, site.roster.size());
        final List<SettlerLeftEvent> left = fired(SettlerLeftEvent.class);
        assertEquals(List.of(SettlerLeaveReason.DISMISSED, SettlerLeaveReason.UNHAPPY, SettlerLeaveReason.UNPAID),
                left.stream().map(SettlerLeftEvent::getReason).toList());
        assertEquals("core.settler.not_found", reason(service.dismiss(CAMP, dismissed.getId())));
        assertEquals("core.settler.not_found",
                reason(service.remove(CAMP, unhappy.getId(), SettlerLeaveReason.UNHAPPY)));
        assertEquals(3, fired(SettlerLeftEvent.class).size());
    }

    @Test
    void ac14_departuresAreRememberedForAWeek() {
        final Settler first = granted("builder");
        first.setRarity(SettlerRarity.RARE);
        final Settler second = granted("farmer");
        final Settler third = granted(null);

        service.remove(CAMP, first.getId(), SettlerLeaveReason.UNHAPPY);
        now.set(1_000 + DAY);
        service.dismiss(CAMP, second.getId());

        assertEquals(List.of(new SettlerDeparture("Tamsin Reed", SettlerRarity.RARE, SettlerLeaveReason.UNHAPPY, 1_000),
                        new SettlerDeparture("Tamsin Reed", SettlerRarity.COMMON, SettlerLeaveReason.DISMISSED, 1_000 + DAY)),
                site.roster.getDepartures());
        assertEquals(1, site.roster.departedSince(0, SettlerLeaveReason.UNHAPPY).size());
        assertEquals(0, site.roster.departedSince(2_000, SettlerLeaveReason.UNHAPPY).size());
        assertEquals(1, site.roster.departedSince(2_000, SettlerLeaveReason.DISMISSED).size());

        now.set(1_000 + 8 * DAY);
        service.remove(CAMP, third.getId(), SettlerLeaveReason.UNPAID);
        assertEquals(List.of(SettlerLeaveReason.DISMISSED, SettlerLeaveReason.UNPAID),
                site.roster.getDepartures().stream().map(SettlerDeparture::getReason).toList(),
                "departures older than a week are dropped");
    }

    @Test
    void ac10_removingForAReasonIgnoresARunningJob() {
        final Settler builder = granted("builder");
        service.assign(CAMP, builder.getId(), "job-a");
        site.running.add(builder.getId());

        assertTrue(service.remove(CAMP, builder.getId(), SettlerLeaveReason.UNPAID).isSuccess());
        assertTrue(site.roster.find(builder.getId()).isEmpty());
    }

    @Test
    void ac15_aSettlerThatLeavesIsGoneAndTheEventCarriesIt() {
        final Settler builder = granted("builder");
        service.assign(CAMP, builder.getId(), "job-a");

        service.remove(CAMP, builder.getId(), SettlerLeaveReason.UNHAPPY);

        assertTrue(site.roster.find(builder.getId()).isEmpty());
        assertEquals(0, site.roster.size());
        final SettlerLeftEvent left = fired(SettlerLeftEvent.class).getFirst();
        assertSame(builder, left.getSettler());
        assertEquals(CAMP, left.getSite());
        assertEquals(SettlerLeaveReason.UNHAPPY, left.getReason());
    }

    private static final class FakeSite implements SettlerSite {

        private final Roster roster = new Roster();
        private final Map<String, Integer> caps = new HashMap<>();
        private final Set<UUID> running = new HashSet<>();
        private final List<SettlerAction> asked = new ArrayList<>();
        private int population = 10;
        private boolean loaded = true;
        private boolean allowed = true;
        private int changes;
        private int runningAsked;

        @Override
        public Optional<Roster> roster(SiteKey site) {
            return loaded ? Optional.of(roster) : Optional.empty();
        }

        @Override
        public void changed(SiteKey site) {
            changes++;
        }

        @Override
        public int populationCap(SiteKey site) {
            return population;
        }

        @Override
        public OptionalInt workingCap(SiteKey site, String profession) {
            final Integer cap = caps.get(profession);
            return cap == null ? OptionalInt.empty() : OptionalInt.of(cap);
        }

        @Override
        public boolean allows(Player player, SiteKey site, SettlerAction action) {
            asked.add(action);
            return allowed;
        }

        @Override
        public SettlerLook look(SiteKey site, Settler settler) {
            return new SettlerLook("model", null, "idle", "walk", "work", 1);
        }

        @Override
        public boolean jobRunning(SiteKey site, Settler settler) {
            runningAsked++;
            return running.contains(settler.getId());
        }
    }
}
