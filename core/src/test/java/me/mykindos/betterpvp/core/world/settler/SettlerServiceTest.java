package me.mykindos.betterpvp.core.world.settler;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class SettlerServiceTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);

    private final AtomicLong now = new AtomicLong(1_000);
    private final List<Event> events = new ArrayList<>();
    private final FakeSite site = new FakeSite();
    private final ProfessionRegistry professions = new ProfessionRegistry();

    private MockedStatic<Bukkit> bukkit;
    private SettlerService service;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        professions.register(Profession.construction("builder", "builder", "model", List.of()));
        professions.register(Profession.workplace("farmer", "farmer", "farm", "model"));
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

    @Test
    void grantingJoinsUntilThePopulationCap() {
        site.population = 2;

        assertTrue(service.grant(CAMP, settler("builder")).isSuccess());
        assertTrue(service.grant(CAMP, settler(null)).isSuccess());
        assertFalse(service.grant(CAMP, settler("farmer")).isSuccess());

        assertEquals(2, site.roster.size());
        assertEquals(2, events.stream().filter(event -> event instanceof SettlerJoinedEvent).count());
        assertEquals(1_000, site.roster.getSettlers().getFirst().getJoinedAt());
        assertTrue(site.changes > 0);
    }

    @Test
    void grantingNeedsALoadedRoster() {
        site.loaded = false;
        assertFalse(service.grant(CAMP, settler("builder")).isSuccess());
        assertFalse(service.grant(SiteKey.of("elsewhere", 1), settler("builder")).isSuccess());
    }

    @Test
    void theWorkingCapLimitsHowManyWorkNotHowManyLive() {
        site.caps.put("builder", 1);
        final Settler first = service.grant(CAMP, settler("builder")).getSettler();
        final Settler second = service.grant(CAMP, settler("builder")).getSettler();

        assertTrue(service.assign(CAMP, first.getId(), "job-a").isSuccess());
        assertFalse(service.assign(CAMP, second.getId(), "job-a").isSuccess());
        // Moving a settler that already works does not count it twice.
        assertTrue(service.assign(CAMP, first.getId(), "job-b").isSuccess());
        assertEquals(SettlerState.WORKING, first.getState());
        assertEquals(SettlerState.IDLE, second.getState());

        assertTrue(service.unassign(CAMP, first.getId()).isSuccess());
        assertTrue(service.assign(CAMP, second.getId(), "job-a").isSuccess());
    }

    @Test
    void settlersOnlyWorkWhereTheirProfessionDoes() {
        final Settler farmer = service.grant(CAMP, settler("farmer")).getSettler();
        final Settler wanderer = service.grant(CAMP, settler(null)).getSettler();

        assertFalse(service.assign(CAMP, farmer.getId(), "job-a").isSuccess());
        assertTrue(service.assign(CAMP, farmer.getId(), "farm").isSuccess());
        assertFalse(service.assign(CAMP, wanderer.getId(), "farm").isSuccess());
    }

    @Test
    void strikersWillNotBeAssigned() {
        final Settler builder = service.grant(CAMP, settler("builder")).getSettler();
        builder.changeState(SettlerState.STRIKING, 5);

        assertFalse(service.assign(CAMP, builder.getId(), "job-a").isSuccess());
    }

    @Test
    void assigningAndUnassigningFireEventsWithWhereItWas() {
        final Settler builder = service.grant(CAMP, settler("builder")).getSettler();
        now.set(2_000);
        service.assign(CAMP, builder.getId(), "job-a");
        service.unassign(CAMP, builder.getId());

        final List<SettlerAssignedEvent> assigned = events.stream()
                .filter(event -> event instanceof SettlerAssignedEvent)
                .map(SettlerAssignedEvent.class::cast)
                .toList();
        assertEquals(2, assigned.size());
        assertNull(assigned.get(0).getPrevious());
        assertEquals("job-a", assigned.get(1).getPrevious());
        assertNull(builder.getAssignment());
        assertEquals(2_000, builder.getStateSince());
    }

    @Test
    void dismissingRemovesItForGood() {
        final Settler builder = service.grant(CAMP, settler("builder")).getSettler();

        assertTrue(service.dismiss(CAMP, builder.getId()).isSuccess());
        assertEquals(0, site.roster.size());
        assertFalse(service.dismiss(CAMP, builder.getId()).isSuccess());
        final SettlerLeftEvent left = (SettlerLeftEvent) events.getLast();
        assertEquals(SettlerLeaveReason.DISMISSED, left.getReason());
    }

    @Test
    void aRosterSurvivesARoundTrip() throws Exception {
        final Settler builder = service.grant(CAMP, settler("builder")).getSettler();
        builder.setSpecialty("mason");
        builder.setTraits(new ArrayList<>(List.of("foreman", "greedy")));
        builder.setHistory("history.dock");
        builder.setRarity(SettlerRarity.RARE);
        service.assign(CAMP, builder.getId(), "job-a");

        final ObjectMapper mapper = new ObjectMapper();
        final Roster read = mapper.readValue(mapper.writeValueAsString(site.roster), Roster.class);

        final Settler copy = read.find(builder.getId()).orElseThrow();
        assertEquals(builder, copy);
        assertEquals(1, read.working("builder"));
    }

    private static final class FakeSite implements SettlerSite {

        private final Roster roster = new Roster();
        private final Map<String, Integer> caps = new HashMap<>();
        private int population = 10;
        private boolean loaded = true;
        private int changes;

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
    }
}
