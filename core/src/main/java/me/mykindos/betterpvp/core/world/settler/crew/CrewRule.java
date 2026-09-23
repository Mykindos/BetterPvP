package me.mykindos.betterpvp.core.world.settler.crew;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.JobRule;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Jobs are worked by crews of Builders. A job waits until its crew's Workforce meets the threshold of the stage it
 * works on, and runs as fast as the crew does together. Repairs and moves need half the threshold of the stage the
 * structure stands at. A Builder on strike brings nothing.
 */
@Singleton
public class CrewRule implements JobRule {

    public static final String ID = "crew";

    private final SettlerService settlers;
    private final StructureCatalogue catalogue;

    @Inject
    public CrewRule(@NotNull SettlerService settlers, @NotNull StructureCatalogue catalogue) {
        this.settlers = settlers;
        this.catalogue = catalogue;
    }

    @Override
    public @NotNull String id() {
        return ID;
    }

    @Override
    public boolean holds(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job) {
        return workforce(site, structure, job) < threshold(structure, job);
    }

    @Override
    public double rate(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job) {
        final List<BuilderStats> stats = stats(site, structure, job);
        return stats.isEmpty() ? 1.0 : CrewSpeed.of(stats, limits(site));
    }

    /** How much Workforce {@code job} needs. */
    public int threshold(@NotNull PlacedStructure structure, @NotNull Job job) {
        final int stage = job.getKind() == JobKind.REPAIR || job.getKind() == JobKind.MOVE
                ? structure.getStage() : job.getTargetStage();
        final int workforce = catalogue.find(structure.getType())
                .filter(type -> type.hasStage(stage))
                .map(type -> type.stage(stage).getWorkforce())
                .orElse(0);
        return job.getKind() == JobKind.REPAIR || job.getKind() == JobKind.MOVE
                ? (workforce + 1) / 2 : workforce;
    }

    public int workforce(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job) {
        return CrewSpeed.workforce(stats(site, structure, job));
    }

    /** How many times faster than its listed time the crew would run {@code job}, or 0 with no crew. */
    public double speed(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job) {
        return CrewSpeed.of(stats(site, structure, job), limits(site));
    }

    /** The settlers on {@code job}'s crew as they are now. Anyone no longer on the roster is left out. */
    public @NotNull List<Settler> crew(@NotNull SiteKey site, @NotNull Job job) {
        final Roster roster = settlers.roster(site).orElse(null);
        if (roster == null) {
            return List.of();
        }
        return job.getStaff().stream()
                .map(roster::find)
                .flatMap(Optional::stream)
                .toList();
    }

    /** What each working member of the crew brings, leaving out anyone on strike. */
    public @NotNull List<BuilderStats> stats(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                             @NotNull Job job) {
        final SettlerSite owner = settlers.site(site).orElse(null);
        if (owner == null) {
            return List.of();
        }
        final List<Settler> crew = crew(site, job).stream()
                .filter(settler -> settler.getState() != SettlerState.STRIKING)
                .toList();
        return crew.stream()
                .map(settler -> owner.builderStats(site, settler, structure, job, crew))
                .flatMap(Optional::stream)
                .toList();
    }

    public @NotNull CrewLimits limits(@NotNull SiteKey site) {
        return settlers.site(site).map(owner -> owner.crewLimits(site)).orElse(CrewLimits.NONE);
    }
}
