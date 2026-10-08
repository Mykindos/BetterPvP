package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Every construction action a player can take, checked and paid for: build, cancel, claim, move, advance, repair,
 * demolish and fitting an upgrade. Each action finds the {@link Worksite} the world belongs to, runs the
 * {@link ConstructionChecks} for it, pays through the site's ledger, and fires events for whatever shows the result in
 * the world.
 */
@Singleton
public class ConstructionService {

    private final ConstructionSites sites;
    private final ConstructionChecks checks;
    private final StructureStatusTracker tracker;
    private final StructureShapes shapes;

    @Inject
    public ConstructionService(@NotNull ConstructionSites sites, @NotNull ConstructionChecks checks,
                               @NotNull StructureStatusTracker tracker, @NotNull StructureShapes shapes) {
        this.sites = sites;
        this.checks = checks;
        this.tracker = tracker;
        this.shapes = shapes;
    }

    ConstructionService(@NotNull SiteInstances instances, @NotNull StructureCatalogue catalogue,
                        @NotNull StructureShapes shapes, @NotNull FitCheck fitCheck, @NotNull LongSupplier clock) {
        this.sites = new ConstructionSites(instances, catalogue);
        this.checks = new ConstructionChecks(sites, catalogue, shapes, fitCheck);
        this.tracker = new StructureStatusTracker(catalogue, clock);
        this.shapes = shapes;
    }

    public void register(@NotNull String siteId, @NotNull ConstructionSite site) {
        sites.register(siteId, site);
    }

    public @NotNull Optional<Worksite> worksite(@NotNull World world) {
        return sites.worksite(world);
    }

    public @NotNull Optional<Component> problem(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                                @NotNull Location anchor, int quarterTurns) {
        return checks.problem(player, world, type, anchor, quarterTurns);
    }

    public @NotNull Optional<Component> unavailable(@NotNull Player player, @NotNull SiteKey site,
                                                    @NotNull StructureType type) {
        return checks.unavailable(player, site, type);
    }

    public @NotNull Optional<Component> moveProblem(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                                    @NotNull Location anchor, int quarterTurns) {
        return checks.moveProblem(player, world, id, anchor, quarterTurns);
    }

    public @NotNull Optional<Component> upgradeUnavailable(@NotNull Player player, @NotNull SiteKey site,
                                                           @NotNull UUID id, @NotNull String upgradeId) {
        return checks.upgradeUnavailable(player, site, id, upgradeId);
    }

    public @NotNull ResourceCost demolishRefund(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                                @NotNull StructureType type) {
        return sites.demolishRefund(site, structure, type);
    }

    public boolean canUse(@NotNull Player player, @NotNull SiteKey site, @NotNull PlacedStructure structure) {
        return sites.canUse(player, site, structure);
    }

    public void refresh(@NotNull Worksite worksite) {
        tracker.refresh(worksite);
    }

    public long now() {
        return tracker.now();
    }

    public @NotNull ConstructionResult build(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                             @NotNull Location anchor, int quarterTurns) {
        final StructurePosition position = StructurePosition.of(anchor, quarterTurns);
        return checks.locate(world, ConstructionResult::refused, worksite -> {
            final Optional<Component> problem = checks.buildProblem(player, worksite, type, position);
            if (problem.isPresent()) {
                return ConstructionResult.refused(problem.get());
            }
            final StructureStage first = type.stage(0);
            final Optional<ConstructionResult> unpaid = pay(worksite, first.getCost());
            if (unpaid.isPresent()) {
                return unpaid.get();
            }

            final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type.getId(), position,
                    StructureCondition.UNDER_CONSTRUCTION);
            structure.setJob(Job.start(JobKind.BUILD, first.getBuildTime(), first.getCost(), 0, now()));
            add(worksite, structure);
            return ConstructionResult.done(structure);
        });
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
                return ConstructionResult.refused("core.construction.nothing_to_cancel");
            }
            if (job.isDone(now())) {
                return ConstructionResult.refused("core.construction.cancel_finished");
            }

            worksite.getSite().ledger().refund(worksite.getKey(), job.getSpent());
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
            if (job == null || job.isHeld() || !job.isDone(now())) {
                return ConstructionResult.refused("core.construction.not_finished");
            }

            switch (job.getKind()) {
                case BUILD -> structure.setCondition(type.getFlags().isStartsBroken()
                        ? StructureCondition.NEEDS_REPAIR : StructureCondition.ACTIVE);
                case ADVANCE -> structure.setStage(job.getTargetStage());
                case MOVE -> structure.setPosition(job.getTarget());
                case REPAIR -> StructureStatusTracker.repaired(structure);
                case FIT_UPGRADE -> {
                }
            }
            structure.setJob(null);
            if (job.getKind() == JobKind.FIT_UPGRADE && job.getUpgrade() != null) {
                type.upgrade(job.getUpgrade()).ifPresent(upgrade -> fitted(worksite, structure, upgrade));
            }
            final ConstructionResult result = changed(worksite, structure);
            UtilServer.callEvent(new StructureClaimedEvent(worksite.getKey(), world, structure, job, player));
            return result;
        });
    }

    /** Moves a structure, or places one that was not placed back into the world. */
    public @NotNull ConstructionResult move(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull Location anchor, int quarterTurns) {
        return act(player, world, id, ConstructionAction.MOVE, (worksite, structure, type) -> {
            final StructurePosition target = StructurePosition.of(anchor, quarterTurns);
            final Optional<Component> problem = checks.moveProblem(worksite, structure, type, target);
            if (problem.isPresent()) {
                return ConstructionResult.refused(problem.get());
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

            final Job job = Job.start(JobKind.MOVE, time, type.getMoveCost(), structure.getStage(), now());
            job.setTarget(target);
            structure.setJob(job);
            return changed(worksite, structure);
        });
    }

    public @NotNull ConstructionResult advance(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.ADVANCE, this::advancing);
    }

    /**
     * Advances structure {@code id} on the site's own behalf, with no player whose permission to check. Requirements,
     * fit and cost are checked and paid as usual.
     */
    public @NotNull ConstructionResult advance(@NotNull World world, @NotNull UUID id) {
        return act(world, id, this::advancing);
    }

    public @NotNull ConstructionResult repair(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.REPAIR, this::repairing);
    }

    /** Repairs structure {@code id} on the site's own behalf, with no player whose permission to check. */
    public @NotNull ConstructionResult repair(@NotNull World world, @NotNull UUID id) {
        return act(world, id, this::repairing);
    }

    /**
     * Finishes the job on structure {@code id} at once, whatever holds it, so it waits to be claimed. Nobody's
     * permission is checked and nothing is paid, so the caller decides who may and what it costs.
     */
    public @NotNull ConstructionResult finish(@NotNull World world, @NotNull UUID id) {
        return act(world, id, (worksite, structure, type) -> {
            final Job job = structure.getJob();
            if (job == null || job.isDone(now())) {
                return ConstructionResult.refused("core.construction.nothing_to_finish");
            }
            job.finish(now());
            return changed(worksite, structure);
        });
    }

    private @NotNull ConstructionResult advancing(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                                  @NotNull StructureType type) {
        final int next = structure.getStage() + 1;
        if (!type.hasStage(next)) {
            return ConstructionResult.refused("core.construction.max_stage");
        }
        if (structure.getJob() != null || structure.getCondition() != StructureCondition.ACTIVE) {
            return ConstructionResult.refused("core.construction.advance_needs_idle");
        }

        final Optional<Component> problem = checks.requirements(worksite.siteHolding(), type, next)
                .or(() -> checks.fit(worksite, type, next, structure.getPosition(), structure.getId()));
        if (problem.isPresent()) {
            return ConstructionResult.refused(problem.get());
        }
        final StructureStage stage = type.stage(next);
        final Optional<ConstructionResult> unpaid = pay(worksite, stage.getCost());
        if (unpaid.isPresent()) {
            return unpaid.get();
        }

        structure.setJob(Job.start(JobKind.ADVANCE, stage.getBuildTime(), stage.getCost(), next, now()));
        return changed(worksite, structure);
    }

    private @NotNull ConstructionResult repairing(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                                  @NotNull StructureType type) {
        final StructureCondition condition = structure.getCondition();
        if (condition != StructureCondition.DISABLED && condition != StructureCondition.NEEDS_REPAIR) {
            return ConstructionResult.refused("core.construction.no_repair_needed");
        }
        if (structure.getJob() != null) {
            return ConstructionResult.refused("core.construction.already_working");
        }
        final Optional<ConstructionResult> unpaid = pay(worksite, type.getRepairCost());
        if (unpaid.isPresent()) {
            return unpaid.get();
        }

        if (type.getRepairTime().isZero()) {
            StructureStatusTracker.repaired(structure);
        } else {
            structure.setJob(Job.start(JobKind.REPAIR, type.getRepairTime(), type.getRepairCost(),
                    structure.getStage(), now()));
        }
        return changed(worksite, structure);
    }

    /**
     * Fits one of a structure's upgrades. It takes one upgrade from each stage it has reached, while it stands working
     * with nothing else under way. An upgrade that takes no time is fitted at once, otherwise it is a job claimed like
     * any other.
     */
    public @NotNull ConstructionResult upgrade(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                               @NotNull String upgradeId) {
        return act(player, world, id, ConstructionAction.PICK_UPGRADE, (worksite, structure, type) ->
                checks.upgrade(structure, type, upgradeId, ConstructionResult::refused, upgrade -> {
                    final Optional<ConstructionResult> unpaid = pay(worksite, upgrade.getCost());
                    if (unpaid.isPresent()) {
                        return unpaid.get();
                    }

                    if (upgrade.getTime().isZero()) {
                        fitted(worksite, structure, upgrade);
                        return changed(worksite, structure);
                    }
                    final Job job = Job.start(JobKind.FIT_UPGRADE, upgrade.getTime(), upgrade.getCost(),
                            structure.getStage(), now());
                    job.setUpgrade(upgrade.getId());
                    structure.setJob(job);
                    return changed(worksite, structure);
                }));
    }

    /**
     * Takes a finished structure down. A share of what it cost comes back, and everything it held is dropped where it
     * stood once it is gone from the holding.
     */
    public @NotNull ConstructionResult demolish(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.DEMOLISH, (worksite, structure, type) -> {
            if (!type.getFlags().isDemolishable()) {
                return ConstructionResult.refused("core.construction.not_demolishable");
            }
            if (structure.getCondition() == StructureCondition.UNDER_CONSTRUCTION
                    || structure.getCondition() == StructureCondition.NOT_PLACED) {
                return ConstructionResult.refused("core.construction.demolish_needs_standing");
            }
            if (structure.getJob() != null) {
                return ConstructionResult.refused("core.construction.busy");
            }

            final Location centre = shapes.placementOf(world, structure)
                    .map(placement -> placement.blockBounds().getCenter().toLocation(world))
                    .orElseGet(() -> structure.getPosition().toLocation(world));
            worksite.getSite().ledger().refund(worksite.getKey(),
                    sites.demolishRefund(worksite.getKey(), structure, type));
            remove(worksite, structure, true);
            for (StructureContents contents : worksite.getSite().contents()) {
                contents.drop(worksite.getKey(), structure, centre);
            }
            StructureStorage.drop(structure, centre);
            return ConstructionResult.done(structure);
        });
    }

    /**
     * Knocks a working structure out until it is repaired, on the site's own behalf. One whose type repairs itself
     * comes back once its repair time has passed.
     */
    public @NotNull ConstructionResult disable(@NotNull World world, @NotNull UUID id) {
        return act(world, id, (worksite, structure, type) -> {
            if (structure.getCondition() != StructureCondition.ACTIVE) {
                return ConstructionResult.refused("core.construction.disable_needs_active");
            }
            structure.setCondition(StructureCondition.DISABLED);
            structure.setDisabledAt(now());
            return changed(worksite, structure);
        });
    }

    private void fitted(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                        @NotNull StructureUpgrade upgrade) {
        structure.getUpgrades().put(upgrade.getStage(), upgrade.getId());
        UtilServer.callEvent(new StructureUpgradedEvent(worksite.getKey(), structure, upgrade));
    }

    /** Spends {@code cost} from the site's ledger, or gives the refusal if it cannot afford it. */
    private @NotNull Optional<ConstructionResult> pay(@NotNull Worksite worksite, @NotNull ResourceCost cost) {
        final Optional<Component> unaffordable = checks.afford(worksite.siteHolding(), cost);
        if (unaffordable.isPresent()) {
            return Optional.of(ConstructionResult.refused(unaffordable.get()));
        }
        worksite.getSite().ledger().spend(worksite.getKey(), cost);
        return Optional.empty();
    }

    /** Runs {@code body} on structure {@code id} on the site's own behalf, with no permission to check. */
    private @NotNull ConstructionResult act(@NotNull World world, @NotNull UUID id, @NotNull Action body) {
        final Optional<Worksite> found = sites.worksite(world);
        if (found.isEmpty()) {
            return ConstructionResult.refused("core.construction.no_holding");
        }
        final Worksite worksite = found.get();
        return checks.resolve(worksite.getHolding(), id, ConstructionResult::refused,
                (structure, type) -> body.run(worksite, structure, type));
    }

    /** Runs {@code body} on structure {@code id} once {@code player} is allowed {@code action} on its site. */
    private @NotNull ConstructionResult act(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull ConstructionAction action, @NotNull Action body) {
        return act(world, id, (worksite, structure, type) -> checks.allowed(player, worksite.siteHolding(), action)
                .map(ConstructionResult::refused)
                .orElseGet(() -> body.run(worksite, structure, type)));
    }

    private void add(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        worksite.getHolding().getStructures().add(structure);
        tracker.applyRules(worksite, structure);
        tracker.track(structure);
        worksite.getSite().changed(worksite.getKey());
        UtilServer.callEvent(new StructurePlacedEvent(worksite.getKey(), structure));
    }

    private void remove(@NotNull Worksite worksite, @NotNull PlacedStructure structure, boolean demolished) {
        worksite.getHolding().getStructures().remove(structure);
        tracker.forget(structure);
        worksite.getSite().changed(worksite.getKey());
        UtilServer.callEvent(new StructureRemovedEvent(worksite.getKey(), structure, demolished));
    }

    private @NotNull ConstructionResult changed(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        tracker.applyRules(worksite, structure);
        worksite.getSite().changed(worksite.getKey());
        tracker.publish(worksite, structure);
        return ConstructionResult.done(structure);
    }

    @FunctionalInterface
    private interface Action {
        @NotNull ConstructionResult run(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                        @NotNull StructureType type);
    }
}
