package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
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
 * Every construction action a player can take, checked and paid for: build, cancel, claim, move, advance, repair,
 * demolish and fitting an upgrade. Each action works out which site the world belongs to, asks that site's {@link ConstructionSite} for its
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
            return Optional.of(text("core.construction.cannot_build_here"));
        }
        return buildProblem(player, worksite.get(), type, StructurePosition.of(anchor, quarterTurns));
    }

    /**
     * Why {@code player} could not start building {@code type} on {@code site} wherever it went: permission,
     * requirements and cost, but not where it would stand. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> unavailable(@NotNull Player player, @NotNull SiteKey site,
                                                    @NotNull StructureType type) {
        final ConstructionSite owner = sites.get(site.getSiteId());
        final Optional<Holding> holding = owner == null ? Optional.empty() : owner.holding(site);
        if (holding.isEmpty()) {
            return Optional.of(text("core.construction.no_holding"));
        }
        if (!owner.allows(player, site, ConstructionAction.BUILD)) {
            return Optional.of(text("core.construction.build_not_allowed"));
        }
        return requirements(site, owner, holding.get(), type, 0)
                .or(() -> owner.ledger().canAfford(site, type.stage(0).getCost())
                        ? Optional.empty() : Optional.of(text("core.construction.cannot_afford")));
    }

    public @NotNull ConstructionResult build(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                             @NotNull Location anchor, int quarterTurns) {
        final Optional<Worksite> found = worksite(world);
        if (found.isEmpty()) {
            return ConstructionResult.refused("core.construction.cannot_build_here");
        }
        final Worksite worksite = found.get();
        final StructurePosition position = StructurePosition.of(anchor, quarterTurns);
        final Optional<Component> problem = buildProblem(player, worksite, type, position);
        if (problem.isPresent()) {
            return ConstructionResult.refused(problem.get());
        }

        final StructureStage first = type.stage(0);
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
                return ConstructionResult.refused("core.construction.nothing_to_cancel");
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
                return ConstructionResult.refused("core.construction.not_finished");
            }

            switch (job.getKind()) {
                case BUILD -> structure.setCondition(type.getFlags().isStartsBroken()
                        ? StructureCondition.NEEDS_REPAIR : StructureCondition.ACTIVE);
                case ADVANCE -> structure.setStage(job.getTargetStage());
                case MOVE -> structure.setPosition(job.getTarget());
                case REPAIR -> structure.setCondition(StructureCondition.ACTIVE);
                case FIT_UPGRADE -> {
                }
            }
            structure.setJob(null);
            if (job.getKind() == JobKind.FIT_UPGRADE && job.getUpgrade() != null) {
                type.upgrade(job.getUpgrade()).ifPresent(upgrade -> fitted(worksite, structure, upgrade));
            }
            final ConstructionResult result = changed(worksite, structure);
            UtilServer.callEvent(new StructureClaimedEvent(worksite.key, worksite.world, structure, job, player));
            return result;
        });
    }

    /** Moves a structure, or places one that was not placed back into the world. */
    public @NotNull ConstructionResult move(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull Location anchor, int quarterTurns) {
        return act(player, world, id, ConstructionAction.MOVE, (worksite, structure, type) -> {
            final StructurePosition target = StructurePosition.of(anchor, quarterTurns);
            final Optional<Component> problem = moveProblem(worksite, structure, type, target);
            if (problem.isPresent()) {
                return ConstructionResult.refused(problem.get());
            }
            worksite.site.ledger().spend(worksite.key, type.getMoveCost());

            final Duration time = type.getMoveTime();
            if (time.isZero() || structure.getCondition() == StructureCondition.NOT_PLACED) {
                structure.setPosition(target);
                if (structure.getCondition() == StructureCondition.NOT_PLACED) {
                    structure.setCondition(StructureCondition.ACTIVE);
                }
                return changed(worksite, structure);
            }

            final Job job = Job.start(JobKind.MOVE, time, type.getMoveCost(), structure.getStage(), clock.getAsLong());
            job.setTarget(target);
            structure.setJob(job);
            return changed(worksite, structure);
        });
    }

    /** Why {@code player} could not move structure {@code id} to {@code anchor}, or empty if they could. */
    public @NotNull Optional<Component> moveProblem(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                                    @NotNull Location anchor, int quarterTurns) {
        final ConstructionResult checked = act(player, world, id, ConstructionAction.MOVE, (worksite, structure, type) ->
                moveProblem(worksite, structure, type, StructurePosition.of(anchor, quarterTurns))
                        .map(ConstructionResult::refused)
                        .orElseGet(() -> ConstructionResult.done(structure)));
        return checked.isSuccess() ? Optional.empty() : Optional.ofNullable(checked.getReason());
    }

    public @NotNull ConstructionResult advance(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.ADVANCE, this::advancing);
    }

    /**
     * Advances structure {@code id} on the site's own behalf, with no player whose permission to check. Requirements,
     * fit and cost are checked and paid as usual.
     */
    public @NotNull ConstructionResult advance(@NotNull World world, @NotNull UUID id) {
        return act(null, world, id, ConstructionAction.ADVANCE, this::advancing);
    }

    public @NotNull ConstructionResult repair(@NotNull Player player, @NotNull World world, @NotNull UUID id) {
        return act(player, world, id, ConstructionAction.REPAIR, this::repairing);
    }

    /** Repairs structure {@code id} on the site's own behalf, with no player whose permission to check. */
    public @NotNull ConstructionResult repair(@NotNull World world, @NotNull UUID id) {
        return act(null, world, id, ConstructionAction.REPAIR, this::repairing);
    }

    /**
     * Finishes the job on structure {@code id} at once, whatever holds it, so it waits to be claimed. Nobody's
     * permission is checked and nothing is paid, so the caller decides who may and what it costs.
     */
    public @NotNull ConstructionResult finish(@NotNull World world, @NotNull UUID id) {
        return act(null, world, id, ConstructionAction.CLAIM, (worksite, structure, type) -> {
            final Job job = structure.getJob();
            if (job == null || job.isDone(clock.getAsLong())) {
                return ConstructionResult.refused("core.construction.nothing_to_finish");
            }
            job.finish(clock.getAsLong());
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

        final Optional<Component> problem = requirements(worksite, type, next)
                .or(() -> fit(worksite, type, next, structure.getPosition(), structure.getId()));
        if (problem.isPresent()) {
            return ConstructionResult.refused(problem.get());
        }
        final StructureStage stage = type.stage(next);
        final Optional<ConstructionResult> unpaid = pay(worksite, stage.getCost());
        if (unpaid.isPresent()) {
            return unpaid.get();
        }

        structure.setJob(Job.start(JobKind.ADVANCE, stage.getBuildTime(), stage.getCost(), next, clock.getAsLong()));
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
            structure.setCondition(StructureCondition.ACTIVE);
        } else {
            structure.setJob(Job.start(JobKind.REPAIR, type.getRepairTime(), type.getRepairCost(),
                    structure.getStage(), clock.getAsLong()));
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
        return act(player, world, id, ConstructionAction.PICK_UPGRADE, (worksite, structure, type) -> {
            final Optional<StructureUpgrade> found = type.upgrade(upgradeId);
            if (found.isEmpty()) {
                return ConstructionResult.refused("core.construction.unknown_upgrade");
            }
            final StructureUpgrade upgrade = found.get();
            final Optional<Component> problem = upgradeProblem(structure, upgrade);
            if (problem.isPresent()) {
                return ConstructionResult.refused(problem.get());
            }
            final Optional<ConstructionResult> unpaid = pay(worksite, upgrade.getCost());
            if (unpaid.isPresent()) {
                return unpaid.get();
            }

            if (upgrade.getTime().isZero()) {
                fitted(worksite, structure, upgrade);
                return changed(worksite, structure);
            }
            final Job job = Job.start(JobKind.FIT_UPGRADE, upgrade.getTime(), upgrade.getCost(), structure.getStage(),
                    clock.getAsLong());
            job.setUpgrade(upgrade.getId());
            structure.setJob(job);
            return changed(worksite, structure);
        });
    }

    /**
     * Why {@code player} could not fit {@code upgradeId} to structure {@code id} on {@code site}, or empty if they
     * could. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> upgradeUnavailable(@NotNull Player player, @NotNull SiteKey site,
                                                           @NotNull UUID id, @NotNull String upgradeId) {
        final ConstructionSite owner = sites.get(site.getSiteId());
        final Optional<PlacedStructure> structure = Optional.ofNullable(owner)
                .flatMap(found -> found.holding(site))
                .flatMap(holding -> holding.find(id));
        if (structure.isEmpty()) {
            return Optional.of(text("core.construction.missing_structure"));
        }
        final Optional<StructureUpgrade> upgrade = catalogue.find(structure.get().getType())
                .flatMap(type -> type.upgrade(upgradeId));
        if (upgrade.isEmpty()) {
            return Optional.of(text("core.construction.unknown_upgrade"));
        }
        if (!owner.allows(player, site, ConstructionAction.PICK_UPGRADE)) {
            return Optional.of(text("core.construction.action_not_allowed"));
        }
        return upgradeProblem(structure.get(), upgrade.get())
                .or(() -> owner.ledger().canAfford(site, upgrade.get().getCost())
                        ? Optional.empty() : Optional.of(text("core.construction.cannot_afford")));
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
            worksite.site.ledger().refund(worksite.key, demolishRefund(worksite.key, structure, type));
            remove(worksite, structure, true);
            for (StructureContents contents : worksite.site.contents()) {
                contents.drop(worksite.key, structure, centre);
            }
            StructureStorage.drop(structure, centre);
            return ConstructionResult.done(structure);
        });
    }

    /** What demolishing {@code structure} on {@code site} gives back. */
    public @NotNull ResourceCost demolishRefund(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                                @NotNull StructureType type) {
        final ConstructionSite owner = sites.get(site.getSiteId());
        final double share = owner == null ? type.getFlags().getDemolishRefund()
                : owner.demolishRefund(site, structure, type);
        return type.costUpTo(structure.getStage()).share(share);
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
            return Optional.of(text("core.construction.build_not_allowed"));
        }
        return requirements(worksite, type, 0)
                .or(() -> fit(worksite, type, 0, position, null))
                .or(() -> worksite.site.ledger().canAfford(worksite.key, type.stage(0).getCost())
                        ? Optional.empty() : Optional.of(text("core.construction.cannot_afford")));
    }

    private @NotNull Optional<Component> upgradeProblem(@NotNull PlacedStructure structure,
                                                        @NotNull StructureUpgrade upgrade) {
        if (structure.getStage() < upgrade.getStage()) {
            return Optional.of(text("core.construction.upgrade_locked"));
        }
        if (structure.upgradeAt(upgrade.getStage()).isPresent()) {
            return Optional.of(text("core.construction.upgrade_taken"));
        }
        if (structure.getJob() != null) {
            return Optional.of(text("core.construction.busy"));
        }
        if (structure.getCondition() != StructureCondition.ACTIVE) {
            return Optional.of(text("core.construction.upgrade_needs_active"));
        }
        return Optional.empty();
    }

    private void fitted(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                        @NotNull StructureUpgrade upgrade) {
        structure.getUpgrades().put(upgrade.getStage(), upgrade.getId());
        UtilServer.callEvent(new StructureUpgradedEvent(worksite.key, structure, upgrade));
    }

    private @NotNull Optional<Component> moveProblem(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                                     @NotNull StructureType type, @NotNull StructurePosition target) {
        if (!type.getFlags().isMovable()) {
            return Optional.of(text("core.construction.not_movable"));
        }
        if (structure.getJob() != null) {
            return Optional.of(text("core.construction.busy"));
        }
        return fit(worksite, type, structure.getStage(), target, structure.getId())
                .or(() -> worksite.site.ledger().canAfford(worksite.key, type.getMoveCost())
                        ? Optional.empty() : Optional.of(text("core.construction.cannot_afford")));
    }

    private @NotNull Optional<Component> requirements(@NotNull Worksite worksite, @NotNull StructureType type, int stage) {
        return requirements(worksite.key, worksite.site, worksite.holding, type, stage);
    }

    private @NotNull Optional<Component> requirements(@NotNull SiteKey key, @NotNull ConstructionSite site,
                                                      @NotNull Holding holding, @NotNull StructureType type, int stage) {
        for (String required : type.getRequiredStructures()) {
            if (!holding.hasBuilt(required)) {
                final Component name = catalogue.find(required).map(StructureType::getDisplayName)
                        .orElse(Component.text(required));
                return Optional.of(text("core.construction.requires", name));
            }
        }
        return site.blocked(key, holding, type, stage);
    }

    private @NotNull Optional<Component> fit(@NotNull Worksite worksite, @NotNull StructureType type, int stage,
                                             @NotNull StructurePosition position, @Nullable UUID ignoring) {
        final Optional<SchematicPlacement> placement = shapes.placementOf(worksite.world, type.getId(), stage, position);
        if (placement.isEmpty()) {
            return Optional.of(text("core.construction.no_build"));
        }
        return fitCheck.problem(worksite.world, worksite.holding, type, placement.get(), ignoring);
    }

    private @NotNull Optional<ConstructionResult> pay(@NotNull Worksite worksite, @NotNull ResourceCost cost) {
        if (!worksite.site.ledger().canAfford(worksite.key, cost)) {
            return Optional.of(ConstructionResult.refused("core.construction.cannot_afford"));
        }
        worksite.site.ledger().spend(worksite.key, cost);
        return Optional.empty();
    }

    /** Runs {@code body} on structure {@code id}, first checking that {@code player}, if there is one, may. */
    private @NotNull ConstructionResult act(@Nullable Player player, @NotNull World world, @NotNull UUID id,
                                            @NotNull ConstructionAction action, @NotNull Action body) {
        final Optional<Worksite> found = worksite(world);
        if (found.isEmpty()) {
            return ConstructionResult.refused("core.construction.no_holding");
        }
        final Worksite worksite = found.get();
        final Optional<PlacedStructure> structure = worksite.holding.find(id);
        if (structure.isEmpty()) {
            return ConstructionResult.refused("core.construction.missing_structure");
        }
        final Optional<StructureType> type = catalogue.find(structure.get().getType());
        if (type.isEmpty()) {
            return ConstructionResult.refused("core.construction.unknown_type");
        }
        if (player != null && !worksite.site.allows(player, worksite.key, action)) {
            return ConstructionResult.refused("core.construction.action_not_allowed");
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

    /**
     * Rules govern running jobs only, so a finished one waits to be claimed whatever changes around it.
     *
     * @return whether anything about the job changed
     */
    private boolean applyRules(@NotNull Worksite worksite, @NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        final long now = clock.getAsLong();
        if (job == null || job.isDone(now)) {
            return false;
        }

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

    private static @NotNull Component text(@NotNull String key, @NotNull ComponentLike... args) {
        return Translations.component(key, args).color(NamedTextColor.RED);
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
