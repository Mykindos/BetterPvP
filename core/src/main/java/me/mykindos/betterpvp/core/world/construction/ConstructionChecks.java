package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Why a construction action would be refused. The queries here and the actions in {@link ConstructionService} run the
 * same checks in the same order, so a menu shows the reason the action would give.
 */
@Singleton
public class ConstructionChecks {

    private final ConstructionSites sites;
    private final StructureCatalogue catalogue;
    private final StructureShapes shapes;
    private final FitCheck fitCheck;

    @Inject
    public ConstructionChecks(@NotNull ConstructionSites sites, @NotNull StructureCatalogue catalogue,
                              @NotNull StructureShapes shapes, @NotNull FitCheck fitCheck) {
        this.sites = sites;
        this.catalogue = catalogue;
        this.shapes = shapes;
        this.fitCheck = fitCheck;
    }

    /** Why {@code type} could not be built at {@code anchor} by {@code player}, or empty if it could. */
    public @NotNull Optional<Component> problem(@NotNull Player player, @NotNull World world, @NotNull StructureType type,
                                                @NotNull Location anchor, int quarterTurns) {
        if (!sites.isSite(world)) {
            return Optional.of(text("core.construction.cannot_build_here"));
        }
        return sites.worksite(world)
                .map(worksite -> buildProblem(player, worksite, type, StructurePosition.of(anchor, quarterTurns)))
                .orElseGet(() -> Optional.of(text("core.construction.no_holding")));
    }

    /**
     * Why {@code player} could not start building {@code type} on {@code site} wherever it went: permission,
     * requirements and cost, but not where it would stand. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> unavailable(@NotNull Player player, @NotNull SiteKey site,
                                                    @NotNull StructureType type) {
        return sites.worksite(site)
                .map(worksite -> buildProblem(player, worksite, type, null))
                .orElseGet(() -> Optional.of(text("core.construction.no_holding")));
    }

    /** Why {@code player} could not move structure {@code id} to {@code anchor}, or empty if they could. */
    public @NotNull Optional<Component> moveProblem(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                                    @NotNull Location anchor, int quarterTurns) {
        return resolve(sites.worksite(world).orElse(null), id, Optional::of, (worksite, structure, type) ->
                allowed(player, worksite, ConstructionAction.MOVE)
                        .or(() -> moveProblem(worksite, structure, type, StructurePosition.of(anchor, quarterTurns))));
    }

    /**
     * Why {@code player} could not fit {@code upgradeId} to structure {@code id} on {@code site}, or empty if they
     * could. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> upgradeUnavailable(@NotNull Player player, @NotNull SiteKey site,
                                                           @NotNull UUID id, @NotNull String upgradeId) {
        return resolve(sites.worksite(site).orElse(null), id, Optional::of, (worksite, structure, type) ->
                allowed(player, worksite, ConstructionAction.PICK_UPGRADE)
                        .or(() -> upgradeProblem(worksite, structure, type, upgradeId)));
    }

    /**
     * Runs {@code body} on structure {@code id} in {@code worksite} with its type, or gives {@code refused} the reason
     * there is none: no loaded holding, no such structure, or a type the catalogue does not know.
     */
    <T> @NotNull T resolve(@Nullable Worksite worksite, @NotNull UUID id, @NotNull Function<Component, T> refused,
                           @NotNull OnStructure<T> body) {
        if (worksite == null) {
            return refused.apply(text("core.construction.no_holding"));
        }
        final Optional<PlacedStructure> structure = worksite.getHolding().find(id);
        if (structure.isEmpty()) {
            return refused.apply(text("core.construction.missing_structure"));
        }
        final Optional<StructureType> type = catalogue.find(structure.get().getType());
        if (type.isEmpty()) {
            return refused.apply(text("core.construction.unknown_type"));
        }
        return body.run(worksite, structure.get(), type.get());
    }

    @NotNull Optional<Component> allowed(@NotNull Player player, @NotNull Worksite worksite,
                                         @NotNull ConstructionAction action) {
        if (worksite.getSite().allows(player, worksite.getKey(), action)) {
            return Optional.empty();
        }
        return Optional.of(text(action == ConstructionAction.BUILD
                ? "core.construction.build_not_allowed" : "core.construction.action_not_allowed"));
    }

    /** The build checks, leaving out where it stands when {@code position} is null. */
    @NotNull Optional<Component> buildProblem(@NotNull Player player, @NotNull Worksite worksite,
                                              @NotNull StructureType type, @Nullable StructurePosition position) {
        return allowed(player, worksite, ConstructionAction.BUILD)
                .or(() -> requirements(worksite, type, 0))
                .or(() -> position == null ? Optional.empty() : fit(worksite, type, 0, position, null))
                .or(() -> afford(worksite, type.stage(0).getCost()));
    }

    @NotNull Optional<Component> moveProblem(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                             @NotNull StructureType type, @NotNull StructurePosition target) {
        if (!type.getFlags().isMovable()) {
            return Optional.of(text("core.construction.not_movable"));
        }
        if (structure.getJob() != null) {
            return Optional.of(text("core.construction.busy"));
        }
        return fit(worksite, type, structure.getStage(), target, structure.getId())
                .or(() -> afford(worksite, type.getMoveCost()));
    }

    @NotNull Optional<Component> upgradeProblem(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                                @NotNull StructureType type, @NotNull String upgradeId) {
        final Optional<StructureUpgrade> found = type.upgrade(upgradeId);
        if (found.isEmpty()) {
            return Optional.of(text("core.construction.unknown_upgrade"));
        }
        final StructureUpgrade upgrade = found.get();
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
        return afford(worksite, upgrade.getCost());
    }

    @NotNull Optional<Component> requirements(@NotNull Worksite worksite, @NotNull StructureType type, int stage) {
        for (String required : type.getRequiredStructures()) {
            if (!worksite.getHolding().hasBuilt(required)) {
                final Component name = catalogue.find(required).map(StructureType::getDisplayName)
                        .orElse(Component.text(required));
                return Optional.of(text("core.construction.requires", name));
            }
        }
        return worksite.getSite().blocked(worksite.getKey(), worksite.getHolding(), type, stage);
    }

    @NotNull Optional<Component> fit(@NotNull Worksite worksite, @NotNull StructureType type, int stage,
                                     @NotNull StructurePosition position, @Nullable UUID ignoring) {
        final World world = worksite.getWorld();
        final Optional<SchematicPlacement> placement = world == null ? Optional.empty()
                : shapes.placementOf(world, type.getId(), stage, position);
        if (placement.isEmpty()) {
            return Optional.of(text("core.construction.no_build"));
        }
        return fitCheck.problem(world, worksite.getHolding(), type, placement.get(), ignoring);
    }

    @NotNull Optional<Component> afford(@NotNull Worksite worksite, @NotNull ResourceCost cost) {
        return worksite.getSite().ledger().canAfford(worksite.getKey(), cost)
                ? Optional.empty() : Optional.of(text("core.construction.cannot_afford"));
    }

    private static @NotNull Component text(@NotNull String key, @NotNull ComponentLike... args) {
        return Translations.component(key, args).color(NamedTextColor.RED);
    }

    /** A check or action on one structure, once it is resolved. */
    @FunctionalInterface
    interface OnStructure<T> {
        @NotNull T run(@NotNull Worksite worksite, @NotNull PlacedStructure structure, @NotNull StructureType type);
    }
}
