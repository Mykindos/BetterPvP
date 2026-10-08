package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.ConstructionSite;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
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
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import me.mykindos.betterpvp.core.world.construction.Worksite;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerAssignedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A camp-like site for crew tests. Its Builders bring their morale as Workforce, and Speed and efficiency set per
 * settler. A fake clock drives construction, and the site reports a Builder's job as running the way a camp does.
 * Assignment events reach the crew service, and the camp is open in the world {@code camp_7}.
 */
final class CrewFixture implements AutoCloseable {

    static final SiteKey CAMP = SiteKey.of("camp", 7);

    final ProfessionRegistry professions = new ProfessionRegistry();
    final StructureCatalogue catalogue = new StructureCatalogue();
    final Site site = new Site();
    final Holding holding = new Holding();
    final ConstructionSites sites = mock(ConstructionSites.class);
    final StructureStatusTracker tracker = mock(StructureStatusTracker.class);
    final ConstructionSite constructionSite = mock(ConstructionSite.class);
    final SiteInstances instances = mock(SiteInstances.class);
    final World world = mock(World.class);
    final Worksite worksite;
    final MockedStatic<Bukkit> bukkit;
    final SettlerService settlers;
    final CrewRule rule;
    final CrewService crews;
    long now = 10_000L;

    CrewFixture() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld("camp_7")).thenReturn(world);
        when(instances.forKey(CAMP)).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY)));

        professions.register(Profession.construction("builder", "builder", List.of()));
        professions.register(Profession.workplace("farmer", "farmer", "farm"));
        catalogue.register(new Hall());
        settlers = new SettlerService(professions);
        settlers.register("camp", site);
        rule = new CrewRule(settlers, catalogue);
        crews = new CrewService(sites, tracker, settlers, professions, instances, rule);
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> {
            if (invocation.getArgument(0) instanceof SettlerAssignedEvent assigned) {
                crews.onAssigned(assigned);
            }
            return null;
        }).when(plugins).callEvent(any());
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        worksite = new Worksite(CAMP, constructionSite, holding, world);
        when(tracker.now()).thenAnswer(invocation -> now);
        when(sites.worksite(world)).thenReturn(Optional.of(worksite));
    }

    @Override
    public void close() {
        bukkit.close();
    }

    Settler builder(int workforce) {
        return builder(workforce, 1.0, 1.0, SettlerRarity.COMMON);
    }

    Settler builder(int workforce, double speed, double efficiency, SettlerRarity rarity) {
        final Settler settler = settler("builder");
        settler.setMorale(workforce);
        settler.setRarity(rarity);
        site.speeds.put(settler.getId(), new double[]{speed, efficiency});
        return settler;
    }

    Settler settler(@Nullable String profession) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Brienne Ashford");
        settler.setProfession(profession);
        site.roster.getSettlers().add(settler);
        return settler;
    }

    /** A Great Hall placed in the holding at {@code stage}, with no job. */
    PlacedStructure hall(int stage) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), "hall",
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        structure.setStage(stage);
        holding.getStructures().add(structure);
        return structure;
    }

    /** A hall advancing to stage 1 (Workforce 5), an hour long, started at {@link #now}. */
    PlacedStructure advancing() {
        final PlacedStructure hall = hall(0);
        hall.setJob(Job.start(JobKind.ADVANCE, Duration.ofHours(1), ResourceCost.NONE, 1, now));
        return hall;
    }

    /** Applies the crew rule to the job the way construction does, so it is held or running. */
    void applyRule(@NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        if (rule.holds(CAMP, structure, job)) {
            if (!job.getHolds().contains(CrewRule.ID)) {
                job.hold(CrewRule.ID, now);
            }
        } else if (job.getHolds().contains(CrewRule.ID)) {
            job.release(CrewRule.ID, now);
        }
        job.setRate(rule.rate(CAMP, structure, job), now);
    }

    SettlerResult enlist(@NotNull PlacedStructure structure, @NotNull Settler settler) {
        return crews.enlist(worksite, structure.getId(), settler.getId());
    }

    static String reason(@NotNull SettlerResult result) {
        if (result.isSuccess()) {
            return "done";
        }
        return ((TranslatableComponent) result.getReason()).key();
    }

    final class Site implements SettlerSite {

        final Roster roster = new Roster();
        final Map<UUID, double[]> speeds = new HashMap<>();
        final Map<UUID, Set<String>> compatible = new HashMap<>();
        final Map<UUID, String> trades = new HashMap<>();
        final List<List<Settler>> finished = new ArrayList<>();
        final List<List<Settler>> statsCrews = new ArrayList<>();
        CrewLimits limits = CrewLimits.NONE;
        OptionalInt builderCap = OptionalInt.empty();
        boolean allowed = true;
        boolean loaded = true;
        int changes;

        @Override
        public @NotNull Optional<Roster> roster(@NotNull SiteKey site) {
            return loaded ? Optional.of(roster) : Optional.empty();
        }

        @Override
        public void changed(@NotNull SiteKey site) {
            changes++;
        }

        @Override
        public int populationCap(@NotNull SiteKey site) {
            return 20;
        }

        @Override
        public @NotNull OptionalInt workingCap(@NotNull SiteKey site, @NotNull String profession) {
            return profession.equals("builder") ? builderCap : OptionalInt.empty();
        }

        @Override
        public boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull SettlerAction action) {
            return allowed;
        }

        @Override
        public @NotNull SettlerLook look(@NotNull SiteKey site, @NotNull Settler settler) {
            return new SettlerLook("model", null, "idle", "walk", "work", 1);
        }

        /** On the staff of an unfinished job on the structure it is assigned to, and that job is not held. */
        @Override
        public boolean jobRunning(@NotNull SiteKey site, @NotNull Settler settler) {
            if (settler.getAssignment() == null) {
                return false;
            }
            return holding.getStructures().stream()
                    .filter(structure -> structure.getId().toString().equals(settler.getAssignment()))
                    .map(PlacedStructure::getJob)
                    .anyMatch(job -> job != null && !job.isDone(now) && !job.isHeld()
                            && job.getStaff().contains(settler.getId()));
        }

        /** A Builder with negative morale is one the site gives no stats for. */
        @Override
        public @NotNull Optional<BuilderStats> builderStats(@NotNull SiteKey site, @NotNull Settler settler,
                                                            @NotNull PlacedStructure structure, @NotNull Job job,
                                                            @NotNull List<Settler> crew) {
            statsCrews.add(List.copyOf(crew));
            if (settler.getMorale() < 0) {
                return Optional.empty();
            }
            final double[] speed = speeds.getOrDefault(settler.getId(), new double[]{1.0, 1.0});
            return Optional.of(new BuilderStats(settler.getMorale(), speed[0], speed[1],
                    trades.get(settler.getId()), compatible.getOrDefault(settler.getId(), Set.of())));
        }

        @Override
        public @NotNull CrewLimits crewLimits(@NotNull SiteKey site) {
            return limits;
        }

        @Override
        public void crewFinished(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job,
                                 @NotNull List<Settler> crew) {
            finished.add(List.copyOf(crew));
        }
    }

    /** Stage 0 needs Workforce 2, stage 1 needs 5. The lantern upgrade needs 3 and the flag upgrade none. */
    static final class Hall implements StructureType {

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
        public @NotNull List<StructureUpgrade> getUpgrades() {
            return List.of(new StructureUpgrade("lantern", 0, ResourceCost.NONE, Duration.ofMinutes(5), 3, null),
                    new StructureUpgrade("flag", 0, ResourceCost.NONE, Duration.ofMinutes(5), 0, null));
        }

        @Override
        public @NotNull StructureFlags getFlags() {
            return StructureFlags.builder().build();
        }
    }
}
