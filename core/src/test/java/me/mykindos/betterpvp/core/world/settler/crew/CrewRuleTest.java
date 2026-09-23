package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureFlags;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrewRuleTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);

    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final Site site = new Site();
    private final List<Settler> finished = new ArrayList<>();
    private final ConstructionService construction = mock(ConstructionService.class);

    private MockedStatic<Bukkit> bukkit;
    private SettlerService settlers;
    private CrewRule rule;
    private CrewService crews;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        professions.register(Profession.construction("builder", "builder", List.of()));
        catalogue.register(new Hall());
        settlers = new SettlerService(professions);
        settlers.register("camp", site);
        rule = new CrewRule(settlers, catalogue);
        crews = new CrewService(construction, settlers, professions, mock(SiteInstances.class), rule);
        when(construction.now()).thenReturn(10_000L);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Settler builder(int workforce) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Brienne Ashford");
        settler.setProfession("builder");
        settler.setMorale(workforce);
        site.roster.getSettlers().add(settler);
        return settler;
    }

    private static PlacedStructure hall(int stage) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), "hall",
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        structure.setStage(stage);
        return structure;
    }

    @Test
    void aJobWaitsUntilItsCrewMeetsTheThreshold() {
        final PlacedStructure hall = hall(0);
        final Job job = Job.start(JobKind.ADVANCE, Duration.ofHours(1), ResourceCost.NONE, 1, 0);
        hall.setJob(job);

        assertEquals(5, rule.threshold(hall, job));
        assertTrue(rule.holds(CAMP, hall, job));

        job.getStaff().add(builder(3).getId());
        assertTrue(rule.holds(CAMP, hall, job));
        job.getStaff().add(builder(2).getId());
        assertFalse(rule.holds(CAMP, hall, job));
        assertEquals(2.0, rule.rate(CAMP, hall, job), 1e-9);
    }

    @Test
    void repairsAndMovesNeedHalfTheStageTheyStandAt() {
        final PlacedStructure hall = hall(1);
        assertEquals(3, rule.threshold(hall, Job.start(JobKind.REPAIR, Duration.ofMinutes(5), ResourceCost.NONE, 1, 0)));
        assertEquals(3, rule.threshold(hall, Job.start(JobKind.MOVE, Duration.ofMinutes(5), ResourceCost.NONE, 1, 0)));
    }

    @Test
    void strikersAndLeaversBringNothing() {
        final PlacedStructure hall = hall(0);
        final Job job = Job.start(JobKind.BUILD, Duration.ofHours(1), ResourceCost.NONE, 0, 0);
        hall.setJob(job);
        final Settler striker = builder(2);
        striker.changeState(SettlerState.STRIKING, 0);
        job.getStaff().add(striker.getId());
        job.getStaff().add(UUID.randomUUID());

        assertEquals(0, rule.workforce(CAMP, hall, job));
        assertTrue(rule.holds(CAMP, hall, job));
    }

    @Test
    void aFinishedJobLetsItsCrewGoOnce() {
        final PlacedStructure hall = hall(0);
        final Job job = Job.start(JobKind.BUILD, Duration.ofMillis(1_000), ResourceCost.NONE, 0, 0);
        hall.setJob(job);
        final Settler first = builder(2);
        settlers.assign(CAMP, first.getId(), hall.getId().toString());
        job.getStaff().add(first.getId());
        final Holding holding = new Holding();
        holding.getStructures().add(hall);

        crews.release(CAMP, holding);
        crews.release(CAMP, holding);

        assertNull(first.getAssignment());
        assertEquals(1, first.getJobsFinished());
        assertEquals(List.of(first), finished);
        assertTrue(job.getStaff().isEmpty());
    }

    @Test
    void aRunningJobKeepsItsCrew() {
        final PlacedStructure hall = hall(0);
        final Job job = Job.start(JobKind.BUILD, Duration.ofHours(1), ResourceCost.NONE, 0, 0);
        hall.setJob(job);
        final Settler member = builder(2);
        settlers.assign(CAMP, member.getId(), hall.getId().toString());
        job.getStaff().add(member.getId());
        final Holding holding = new Holding();
        holding.getStructures().add(hall);

        crews.release(CAMP, holding);
        assertEquals(hall.getId().toString(), member.getAssignment());

        hall.setJob(null);
        crews.release(CAMP, holding);
        assertNull(member.getAssignment(), "a cancelled or claimed job lets its crew go");
    }

    /** A site whose Builders bring their morale as Workforce, at speed 1. */
    private final class Site implements SettlerSite {

        private final Roster roster = new Roster();

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
        public @NotNull Optional<BuilderStats> builderStats(@NotNull SiteKey site, @NotNull Settler settler,
                                                            @NotNull PlacedStructure structure, @NotNull Job job,
                                                            @NotNull List<Settler> crew) {
            return Optional.of(new BuilderStats(settler.getMorale(), 1, 1, null, Set.of()));
        }

        @Override
        public void crewFinished(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job,
                                 @NotNull List<Settler> crew) {
            finished.addAll(crew);
        }
    }

    private static final class Hall implements StructureType {

        @Override
        public @NotNull String getId() {
            return "hall";
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.text("Hall");
        }

        @Override
        public int getTier() {
            return 1;
        }

        @Override
        public @NotNull Set<String> getRequiredStructures() {
            return Set.of();
        }

        @Override
        public @Nullable String getRequiredZoneTag() {
            return null;
        }

        @Override
        public @NotNull List<StructureStage> getStages() {
            return List.of(new StructureStage("hall_1", ResourceCost.NONE, Duration.ZERO, 2),
                    new StructureStage("hall_2", ResourceCost.NONE, Duration.ofHours(1), 5));
        }

        @Override
        public @NotNull StructureFlags getFlags() {
            return StructureFlags.builder().build();
        }
    }
}
