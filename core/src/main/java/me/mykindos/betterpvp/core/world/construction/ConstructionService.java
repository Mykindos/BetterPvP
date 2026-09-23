package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Every construction action a player can take, checked and paid for: build, cancel, claim, move, upgrade, repair and
 * demolish. Each action works out which site the world belongs to, asks that site's {@link ConstructionSite} for its
 * holding, costs and permissions, and fires events for whatever shows the result in the world.
 * <p>
 * Structure status is time-driven as well as action-driven (a job finishes by itself), so {@link #refresh} is called
 * regularly for every loaded site to apply job rules and announce status changes.
 */
@Singleton
public class ConstructionService {

    private final SiteInstances instances;
    private final StructureCatalogue catalogue;
    private final StructureShapes shapes;
    private final FitCheck fitCheck;
    private final LongSupplier clock;

    private final Map<String, ConstructionSite> sites = new HashMap<>();
    private final Map<UUID, StructureStatus> lastStatus = new ConcurrentHashMap<>();

    @Inject
    public ConstructionService(@NotNull SiteInstances instances, @NotNull StructureCatalogue catalogue,
                               @NotNull StructureShapes shapes, @NotNull FitCheck fitCheck) {
        this(instances, catalogue, shapes, fitCheck, System::currentTimeMillis);
    }

    ConstructionService(@NotNull SiteInstances instances, @NotNull StructureCatalogue catalogue,
                        @NotNull StructureShapes shapes, @NotNull FitCheck fitCheck, @NotNull LongSupplier clock) {
        this.instances = instances;
        this.catalogue = catalogue;
        this.shapes = shapes;
        this.fitCheck = fitCheck;
        this.clock = clock;
    }

    /** Makes construction possible on every instance of the site {@code siteId}. */
    public void register(@NotNull String siteId, @NotNull ConstructionSite site) {
        sites.put(siteId, site);
    }

    /** The holding a world belongs to, if construction happens there and its record is loaded. */
    public @NotNull Optional<Worksite> worksite(@NotNull World world) {
        return instances.byWorld(world.getName()).map(SiteInstance::getKey).flatMap(key -> {
            final ConstructionSite site = sites.get(key.getSiteId());
            return site == null ? Optional.empty()
                    : site.holding(key).map(holding -> new Worksite(key, site, holding, world));
        });
    }

    /** Why {@code type} could not be built at {@code anchor} by {@code player}, or empty if it could. */
    public @NotNull Optional<Component> problem(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                                @NotNull Location anchor, int quarterTurns) {
        final Optional<Worksite> worksite = worksite(world);
        if (worksite.isEmpty()) {
            return Optional.of(text("You can't build here."));
        }
        return buildProblem(player, worksite.get(), type, StructurePosition.of(anchor, quarterTurns));
    }

    public @NotNull ConstructionResult build(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                             @NotNull Location anchor, int quarterTurns) {
        final Optional<Worksite> found = worksite(world);
        if (found.isEmpty()) {
            return ConstructionResult.refused("You can't build here.");
        }
        final Worksite worksite = found.get();
        final StructurePosition position = StructurePosition.of(anchor, quarterTurns);
        final Optional<Component> problem = buildProblem(player, worksite, type, position);
        if (problem.isPresent()) {
            return ConstructionResult.refused(problem.get());
        }

        final StructureVersion first = type.version(0);
        worksite.site.ledger().spend(worksite.key, first.getCost());
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type.getId(), position,
                StructureCondition.UNDER_CONSTRUCTION);
        structure.setJob(Job.start(JobKind.BUILD, first.getBuildTime(), first.getCost(), 0, clock.getAsLong()));
        add(worksite, structure);
        return ConstructionResult.done(structure);
    }

    /** Puts a structure into a holding with no checks and nothing spent, such as the ones every new holding starts with. */
    public @NotNull PlacedStructure grant(@NotNull Worksite worksite, @NotNull StructureType type,
                                          @NotNull StructurePosition position, @NotNull StructureCondition condition) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type.getId(), position, condition);
        add(worksite, structure);
        return structure;
    }

    public @NotNull ConstructionResult cancel(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.CANCEL, (worksite, structure, type) -> {
            final Job job = structure.getJob();
            if (job == null) {
                return ConstructionResult.refused("There is nothing to cancel.");
            }

            worksite.site.ledger().refund(worksite.key, job.getSpent());
            if (job.getKind() == JobKind.BUILD) {
                remove(worksite, structure, false);
                return ConstructionResult.done(structure);
            }
            structure.setJob(null);
            return changed(worksite, structure);
        });
    }

    public @NotNull ConstructionResult claim(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.CLAIM, (worksite, structure, type) -> {
            final Job job = structure.getJob();
            if (job == null || job.isHeld() || !job.isDone(clock.getAsLong())) {
                return ConstructionResult.refused("It isn't finished yet.");
            }

            switch (job.getKind()) {
                case BUILD -> structure.setCondition(type.getFlags().isStartsBroken()
                        ? StructureCondition.NEEDS_REPAIR : StructureCondition.ACTIVE);
                case UPGRADE -> structure.setVersion(job.getTargetVersion());
                case MOVE -> structure.setPosition(job.getTarget());
                case REPAIR -> structure.setCondition(StructureCondition.ACTIVE);
            }
            structure.setJob(null);
            return changed(worksite, structure);
        });
    }

    /** Moves a structure, or places one that was not placed back into the world. */
    public @NotNull ConstructionResult move(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull Location anchor, int quarterTurns) {
        return act(player, world, id, ConstructionAction.MOVE, (worksite, structure, type) -> {
            if (!type.getFlags().isMovable()) {
                return ConstructionResult.refused("It can't be moved.");
            }
            if (structure.getJob() != null) {
                return ConstructionResult.refused("Finish or cancel what it's doing first.");
            }

            final StructurePosition target = StructurePosition.of(anchor, quarterTurns);
            final Optional<Component> misfit = fit(worksite, type, structure.getVersion(), target, structure.getId());
            if (misfit.isPresent()) {
                return ConstructionResult.refused(misfit.get());
            }
            final Optional<ConstructionResult> unpaid = pay(worksite, type.getMoveCost());
            if (unpaid.isPresent()) {
                return unpaid.get();
            }

            final Duration time = type.getMoveTime();
            if (time.isZero() || structure.getCondition() == StructureCondition.NOT_PLACED) {
                structure.setPosition(target);
                if (structure.getCondition() == StructureCondition.NOT_PLACED) {
                    structure.setCondition(StructureCondition.ACTIVE);
                }
                return changed(worksite, structure);
            }

            final Job job = Job.start(JobKind.MOVE, time, type.getMoveCost(), structure.getVersion(), clock.getAsLong());
            job.setTarget(target);
            structure.setJob(job);
            return changed(worksite, structure);
        });
    }

    public @NotNull ConstructionResult upgrade(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.UPGRADE, (worksite, structure, type) -> {
            final int next = structure.getVersion() + 1;
            if (!type.hasVersion(next)) {
                return ConstructionResult.refused("It can't be upgraded any further.");
            }
            if (structure.getJob() != null || structure.getCondition() != StructureCondition.ACTIVE) {
                return ConstructionResult.refused("It has to be standing and idle to be upgraded.");
            }

            final Optional<Component> problem = requirements(worksite, type, next)
                    .or(() -> fit(worksite, type, next, structure.getPosition(), structure.getId()));
            if (problem.isPresent()) {
                return ConstructionResult.refused(problem.get());
            }
            final StructureVersion version = type.version(next);
            final Optional<ConstructionResult> unpaid = pay(worksite, version.getCost());
            if (unpaid.isPresent()) {
                return unpaid.get();
            }

            structure.setJob(Job.start(JobKind.UPGRADE, version.getBuildTime(), version.getCost(), next, clock.getAsLong()));
            return changed(worksite, structure);
        });
    }

    public @NotNull ConstructionResult repair(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.REPAIR, (worksite, structure, type) -> {
            final StructureCondition condition = structure.getCondition();
            if (condition != StructureCondition.DISABLED && condition != StructureCondition.NEEDS_REPAIR) {
                return ConstructionResult.refused("It doesn't need repairing.");
            }
            if (structure.getJob() != null) {
                return ConstructionResult.refused("It is already being worked on.");
            }
            final Optional<ConstructionResult> unpaid = pay(worksite, type.getRepairCost());
            if (unpaid.isPresent()) {
                return unpaid.get();
            }

            if (type.getRepairTime().isZero()) {
                structure.setCondition(StructureCondition.ACTIVE);
            } else {
                structure.setJob(Job.start(JobKind.REPAIR, type.getRepairTime(), type.getRepairCost(),
                        structure.getVersion(), clock.getAsLong()));
            }
            return changed(worksite, structure);
        });
    }

    /**
     * Takes a finished structure down. A share of what it cost comes back, and everything it held is dropped where it
     * stood once it is gone from the holding.
     */
    public @NotNull ConstructionResult demolish(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.DEMOLISH, (worksite, structure, type) -> {
            if (!type.getFlags().isDemolishable()) {
                return ConstructionResult.refused("It can't be demolished.");
            }
            if (structure.getCondition() == StructureCondition.UNDER_CONSTRUCTION
                    || structure.getCondition() == StructureCondition.NOT_PLACED) {
                return ConstructionResult.refused("Only a standing structure can be demolished.");
            }
            if (structure.getJob() != null) {
                return ConstructionResult.refused("Cancel what it's doing first.");
            }

            final Location centre = shapes.placementOf(world, structure)
                    .map(placement -> placement.blockBounds().getCenter().toLocation(world))
                    .orElseGet(() -> structure.getPosition().toLocation(world));
            worksite.site.ledger().refund(worksite.key,
                    type.costUpTo(structure.getVersion()).share(type.getFlags().getDemolishRefund()));
            remove(worksite, structure, true);
            for (StructureContents contents : worksite.site.contents()) {
                contents.drop(worksite.key, structure, centre);
            }
            return ConstructionResult.done(structure);
        });
    }

    /** Applies job rules and announces any status that changed, for every structure on one site. */
    public void refresh(@NotNull Worksite worksite) {
        for (PlacedStructure structure : worksite.holding.getStructures()) {
            if (applyRules(worksite, structure)) {
                worksite.site.changed(worksite.key);
            }
            publish(worksite, structure);
        }
    }

    public long now() {
        return clock.getAsLong();
    }

    private @NotNull Optional<Component> buildProblem(@NotNull Player player, @NotNull Worksite worksite,
                                                      @NotNull StructureType type, @NotNull StructurePosition position) {
        if (!worksite.site.allows(player, worksite.key, ConstructionAction.BUILD)) {
            return Optional.of(text("You aren't allowed to build here."));
        }
        return requirements(worksite, type, 0)
                .or(() -> fit(worksite, type, 0, position, null))
                .or(() -> worksite.site.ledger().canAfford(worksite.key, type.version(0).getCost())
                        ? Optional.empty() : Optional.of(text("You don't have enough resources.")));
    }

    private @NotNull Optional<Component> requirements(@NotNull Worksite worksite, @NotNull StructureType type, int version) {
        for (String required : type.getRequiredStructures()) {
            if (!worksite.holding.hasBuilt(required)) {
                final Component name = catalogue.find(required).map(StructureType::getDisplayName)
                        .orElse(Component.text(required));
                return Optional.of(text("It needs a ").append(name).append(text(" first.")));
            }
        }
        return worksite.site.blocked(worksite.key, worksite.holding, type, version);
    }

    private @NotNull Optional<Component> fit(@NotNull Worksite worksite, @NotNull StructureType type, int version,
                                             @NotNull StructurePosition position, @Nullable UUID ignoring) {
        final Optional<SchematicPlacement> placement = shapes.placementOf(worksite.world, type.getId(), version, position);
        if (placement.isEmpty()) {
            return Optional.of(text("That structure has no build to place."));
        }
        return fitCheck.problem(worksite.world, worksite.holding, type, placement.get(), ignoring);
    }

    private @NotNull Optional<ConstructionResult> pay(@NotNull Worksite worksite, @NotNull ResourceCost cost) {
        if (!worksite.site.ledger().canAfford(worksite.key, cost)) {
            return Optional.of(ConstructionResult.refused("You don't have enough resources."));
        }
        worksite.site.ledger().spend(worksite.key, cost);
        return Optional.empty();
    }

    private @NotNull ConstructionResult act(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull ConstructionAction action, @NotNull Action body) {
        final Optional<Worksite> found = worksite(world);
        if (found.isEmpty()) {
            return ConstructionResult.refused("There is nothing to build on here.");
        }
        final Worksite worksite = found.get();
        final Optional<PlacedStructure> structure = worksite.holding.find(id);
        if (structure.isEmpty()) {
            return ConstructionResult.refused("That structure no longer exists.");
        }
        final Optional<StructureType> type = catalogue.find(structure.get().getType());
        if (type.isEmpty()) {
            return ConstructionResult.refused("That structure is no longer known.");
        }
        if (!worksite.site.allows(player, worksite.key, action)) {
            return ConstructionResult.refused("You aren't allowed to do that here.");
        }
        return body.run(worksite, structure.get(), type.get());
    }

    private void add(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        worksite.holding.getStructures().add(structure);
        applyRules(worksite, structure);
        lastStatus.put(structure.getId(), structure.status(clock.getAsLong()));
        worksite.site.changed(worksite.key);
        UtilServer.callEvent(new StructurePlacedEvent(worksite.key, structure));
    }

    private void remove(@NotNull Worksite worksite, @NotNull PlacedStructure structure, boolean demolished) {
        worksite.holding.getStructures().remove(structure);
        lastStatus.remove(structure.getId());
        worksite.site.changed(worksite.key);
        UtilServer.callEvent(new StructureRemovedEvent(worksite.key, structure, demolished));
    }

    private @NotNull ConstructionResult changed(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        applyRules(worksite, structure);
        worksite.site.changed(worksite.key);
        publish(worksite, structure);
        return ConstructionResult.done(structure);
    }

    /** @return whether anything about the job changed */
    private boolean applyRules(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        if (job == null) {
            return false;
        }

        final long now = clock.getAsLong();
        boolean changed = false;
        double rate = 1.0;
        for (JobRule rule : worksite.site.jobRules()) {
            final boolean holds = rule.holds(worksite.key, structure, job);
            if (holds && !job.getHolds().contains(rule.id())) {
                job.hold(rule.id(), now);
                changed = true;
            } else if (!holds && job.getHolds().contains(rule.id())) {
                job.release(rule.id(), now);
                changed = true;
            }
            rate *= rule.rate(worksite.key, structure, job);
        }
        if (Math.abs(rate - job.getRate()) > 1e-9) {
            job.setRate(rate, now);
            changed = true;
        }
        return changed;
    }

    private void publish(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        final StructureStatus status = structure.status(clock.getAsLong());
        final StructureStatus previous = lastStatus.put(structure.getId(), status);
        if (previous != null && previous != status) {
            UtilServer.callEvent(new StructureStatusChangeEvent(worksite.key, structure, previous, status));
        }
    }

    private static @NotNull Component text(@NotNull String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    @FunctionalInterface
    private interface Action {
        @NotNull ConstructionResult run(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                        @NotNull StructureType type);
    }

    /** One site's holding in the world it stands in, and what that site's module supplies. */
    @Value
    public static class Worksite {
        SiteKey key;
        ConstructionSite site;
        Holding holding;
        World world;
    }
}
