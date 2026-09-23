package me.mykindos.betterpvp.core.world.settler.morale;

import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MoraleEngineTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(HOUR);
    private final Site site = new Site();

    private MockedStatic<Bukkit> bukkit;
    private MoraleEngine engine;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        final ProfessionRegistry professions = new ProfessionRegistry();
        professions.register(Profession.construction("builder", "builder", List.of()));
        final SettlerService settlers = new SettlerService(professions);
        settlers.register("camp", site);
        engine = new MoraleEngine(settlers, mock(SiteInstances.class), now::get);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Settler settler(int morale) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Mirel of the Fens");
        site.roster.getSettlers().add(settler);
        site.morale.put(settler.getId(), morale);
        return settler;
    }

    @Test
    void moraleFollowsTheModelWithinItsBounds() {
        final Settler happy = settler(250);
        final Settler sad = settler(-10);
        engine.settle(CAMP);

        assertEquals(100, happy.getMorale());
        assertEquals(-10, sad.getMorale());
        assertEquals(1.5, MoraleEngine.multiplier(happy));
        assertEquals(0.95, MoraleEngine.multiplier(sad), 1e-9);
    }

    @Test
    void anUnhappySettlerLeavesOnlyAfterStayingUnhappy() {
        final Settler settler = settler(-50);
        engine.settle(CAMP);
        assertEquals(HOUR, settler.getUnhappySince());

        now.addAndGet(47 * HOUR);
        site.morale.put(settler.getId(), -10);
        engine.settle(CAMP);
        assertEquals(0, settler.getUnhappySince(), "cheering up resets the count");

        site.morale.put(settler.getId(), -50);
        engine.settle(CAMP);
        now.addAndGet(48 * HOUR);
        engine.settle(CAMP);
        assertTrue(site.roster.find(settler.getId()).isEmpty());
        assertEquals(SettlerLeaveReason.UNHAPPY, site.roster.getDepartures().getLast().getReason());
    }

    @Test
    void aSettlerThatNeverLeavesStaysHoweverItFeels() {
        final Settler loyal = settler(-90);
        site.loyal = loyal.getId();
        engine.settle(CAMP);
        now.addAndGet(100 * HOUR);
        engine.settle(CAMP);

        assertTrue(site.roster.find(loyal.getId()).isPresent());
        assertEquals(0, loyal.getUnhappySince());
    }

    private static final class Site implements SettlerSite, MoraleModel {

        private final Roster roster = new Roster();
        private final Map<UUID, Integer> morale = new HashMap<>();
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
            return Optional.of(this);
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
    }
}
