package me.mykindos.betterpvp.core.world.settler.morale;

import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerLeftEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
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

class MoraleEngineTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);
    private static final SiteKey CLOSED = SiteKey.of("camp", 8);
    private static final SiteKey UNLOADED = SiteKey.of("camp", 9);
    private static final long HOUR = 3_600_000L;
    private static final long MINUTE = 60_000L;

    private final AtomicLong now = new AtomicLong(HOUR);
    private final List<Event> events = new ArrayList<>();
    private final Site site = new Site();
    private final SiteInstances instances = mock(SiteInstances.class);

    private MockedStatic<Bukkit> bukkit;
    private MoraleEngine engine;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        final ProfessionRegistry professions = new ProfessionRegistry();
        professions.register(Profession.construction("builder", "builder", List.of()));
        final SettlerService settlers = new SettlerService(professions);
        settlers.register("camp", site);
        engine = new MoraleEngine(settlers, instances, now::get);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Settler settler(int morale) {
        return settler(site.roster, morale);
    }

    private Settler settler(Roster roster, int morale) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Mirel of the Fens");
        roster.getSettlers().add(settler);
        site.morale.put(settler.getId(), morale);
        return settler;
    }

    private void settleAfter(long millis) {
        now.addAndGet(millis);
        engine.settle(CAMP);
    }

    @Test
    void ac10_onlySitesWithALoadedWorldAreSettledAndChangesAreWrittenDown() {
        final Settler open = settler(30);
        final Roster closedRoster = new Roster();
        site.rosters.put(CLOSED, closedRoster);
        final Settler closed = settler(closedRoster, 30);
        when(instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY),
                new SiteInstance(UUID.randomUUID(), CLOSED, "camp_8", SiteInstance.State.DORMANT)));
        bukkit.when(() -> Bukkit.getWorld("camp_7")).thenReturn(mock(World.class));

        engine.tick();
        assertEquals(30, open.getMorale());
        assertEquals(0, closed.getMorale());
        assertTrue(site.changed.contains(CAMP));
        assertFalse(site.changed.contains(CLOSED));
    }

    @Test
    void ac16_moraleFollowsTheModelWithinItsBounds() {
        final Settler happy = settler(250);
        final Settler miserable = settler(-250);
        final Settler sad = settler(-10);
        engine.settle(CAMP);

        assertEquals(100, happy.getMorale());
        assertEquals(-100, miserable.getMorale());
        assertEquals(-10, sad.getMorale());
    }

    @Test
    void ac16_withNoModelOrNoRosterMoraleIsLeftAlone() {
        final Settler settler = settler(-60);
        settler.setMorale(33);
        site.model = false;
        engine.settle(CAMP);
        assertEquals(33, settler.getMorale());
        assertTrue(site.changed.isEmpty());

        engine.settle(UNLOADED);
        assertTrue(site.changed.isEmpty());
    }

    @Test
    void ac17_theMultiplierRunsFromHalfToOneAndAHalf() {
        final Settler settler = new Settler();
        settler.setMorale(-100);
        assertEquals(0.5, MoraleEngine.multiplier(settler));
        settler.setMorale(0);
        assertEquals(1.0, MoraleEngine.multiplier(settler));
        settler.setMorale(100);
        assertEquals(1.5, MoraleEngine.multiplier(settler));
        settler.setMorale(-10);
        assertEquals(0.95, MoraleEngine.multiplier(settler), 1e-9);
    }

    @Test
    void ac18_theUnhappyClockStartsBelowTheLineAndClearsAtIt() {
        final Settler settler = settler(-41);
        engine.settle(CAMP);
        assertEquals(HOUR, settler.getUnhappySince());

        site.morale.put(settler.getId(), -40);
        settleAfter(MINUTE);
        assertEquals(0, settler.getUnhappySince(), "the line itself is not below it");
    }

    @Test
    void ac18_aSettlerThatNeverLeavesHasNoUnhappyClock() {
        final Settler loyal = settler(-90);
        site.loyal = loyal.getId();
        engine.settle(CAMP);
        settleAfter(100 * HOUR);

        assertTrue(site.roster.find(loyal.getId()).isPresent());
        assertEquals(0, loyal.getUnhappySince());
    }

    @Test
    void ac19_aSettlerUnhappyForTheLeaveTimeLeaves() {
        final Settler settler = settler(-50);
        engine.settle(CAMP);

        settleAfter(48 * HOUR - MINUTE);
        assertTrue(site.roster.find(settler.getId()).isPresent());
        settleAfter(MINUTE);
        assertTrue(site.roster.find(settler.getId()).isEmpty());
        assertEquals(SettlerLeaveReason.UNHAPPY, site.roster.getDepartures().getLast().getReason());
        assertTrue(events.stream().anyMatch(event -> event instanceof SettlerLeftEvent left
                && left.getSettler() == settler && left.getReason() == SettlerLeaveReason.UNHAPPY));
    }

    @Test
    void ac19_cheeringUpRestartsTheCount() {
        final Settler settler = settler(-50);
        engine.settle(CAMP);
        site.morale.put(settler.getId(), -10);
        settleAfter(47 * HOUR);
        site.morale.put(settler.getId(), -50);
        settleAfter(MINUTE);
        settleAfter(2 * HOUR);

        assertTrue(site.roster.find(settler.getId()).isPresent());
    }

    @Test
    void ac19_timeWhileTheWorldWasClosedCounts() {
        final Settler settler = settler(-50);
        engine.settle(CAMP);

        settleAfter(48 * HOUR);
        assertTrue(site.roster.find(settler.getId()).isEmpty());
    }

    private static final class Site implements SettlerSite, MoraleModel {

        private final Roster roster = new Roster();
        private final Map<SiteKey, Roster> rosters = new HashMap<>(Map.of(CAMP, roster));
        private final Map<UUID, Integer> morale = new HashMap<>();
        private final List<SiteKey> changed = new ArrayList<>();
        private boolean model = true;
        private UUID loyal;

        @Override
        public int morale(@NotNull SiteKey site, @NotNull Settler settler, @NotNull Roster roster, long now) {
            return morale.getOrDefault(settler.getId(), 0);
        }

        @Override
        public int leaveBelow() {
            return -40;
        }

        @Override
        public @NotNull Duration leaveAfter() {
            return Duration.ofHours(48);
        }

        @Override
        public boolean mayLeave(@NotNull Settler settler) {
            return !settler.getId().equals(loyal);
        }

        @Override
        public @NotNull Optional<MoraleModel> moraleModel(@NotNull SiteKey site) {
            return model ? Optional.of(this) : Optional.empty();
        }

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
    }
}
