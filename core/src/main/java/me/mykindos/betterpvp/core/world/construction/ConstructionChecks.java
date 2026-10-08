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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Why a construction action would be refused. The queries here and the actions in {@link ConstructionService} run the
 * same checks in the same order, with cost always last, so a menu shows the reason the action would give.
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
        final StructurePosition position = StructurePosition.of(anchor, quarterTurns);
        return locate(world, Optional::of, worksite -> buildProblem(player, worksite, type, position)
                .or(() -> afford(worksite.siteHolding(), type.stage(0).getCost())));
    }

    /**
     * Why {@code player} could not start building {@code type} on {@code site} wherever it went: permission,
     * requirements and cost, but not where it would stand. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> unavailable(@NotNull Player player, @NotNull SiteKey site,
                                                    @NotNull StructureType type) {
        return sites.holding(site)
                .map(at -> buildProblem(player, at, type, Optional::empty)
                        .or(() -> afford(at, type.stage(0).getCost())))
                .orElseGet(() -> Optional.of(text("core.construction.no_holding")));
    }

    /** Why {@code player} could not move structure {@code id} to {@code anchor}, or empty if they could. */
    public @NotNull Optional<Component> moveProblem(@NotNull Player player, @NotNull World world, @NotNull UUID id,
                                                    @NotNull Location anchor, int quarterTurns) {
        final StructurePosition target = StructurePosition.of(anchor, quarterTurns);
        return sites.worksite(world)
                .map(worksite -> resolve(worksite.getHolding(), id, Optional::of, (structure, type) ->
                        allowed(player, worksite.siteHolding(), ConstructionAction.MOVE)
                                .or(() -> moveProblem(worksite, structure, type, target))
                                .or(() -> afford(worksite.siteHolding(), type.getMoveCost()))))
                .orElseGet(() -> Optional.of(text("core.construction.no_holding")));
    }

    /**
     * Why {@code player} could not fit {@code upgradeId} to structure {@code id} on {@code site}, or empty if they
     * could. Works whether the site's world is loaded or not.
     */
    public @NotNull Optional<Component> upgradeUnavailable(@NotNull Player player, @NotNull SiteKey site,
                                                           @NotNull UUID id, @NotNull String upgradeId) {
        return sites.holding(site)
                .map(at -> resolve(at.getHolding(), id, Optional::of, (structure, type) ->
                        allowed(player, at, ConstructionAction.PICK_UPGRADE)
                                .or(() -> upgrade(structure, type, upgradeId, Optional::of,
                                        upgrade -> afford(at, upgrade.getCost())))))
                .orElseGet(() -> Optional.of(text("core.construction.no_holding")));
    }

    /**
     * Runs {@code body} on the worksite {@code world} belongs to, or gives {@code refused} the reason there is none:
     * the world is no site, or its holding is not loaded.
     */
    <T> @NotNull T locate(@NotNull World world, @NotNull Function<Component, T> refused,
                          @NotNull Function<Worksite, T> body) {
        if (!sites.isSite(world)) {
            return refused.apply(text("core.construction.cannot_build_here"));
        }
        return sites.worksite(world).map(body).orElseGet(() -> refused.apply(text("core.construction.no_holding")));
    }

    /**
     * Runs {@code body} on structure {@code id} in {@code holding} with its type, or gives {@code refused} the reason
     * there is none: no such structure, or a type the catalogue does not know.
     */
    <T> @NotNull T resolve(@NotNull Holding holding, @NotNull UUID id, @NotNull Function<Component, T> refused,
                           @NotNull BiFunction<PlacedStructure, StructureType, T> body) {
        final Optional<PlacedStructure> structure = holding.find(id);
        if (structure.isEmpty()) {
            return refused.apply(text("core.construction.missing_structure"));
        }
        final Optional<StructureType> type = catalogue.find(structure.get().getType());
        if (type.isEmpty()) {
            return refused.apply(text("core.construction.unknown_type"));
        }
        return body.apply(structure.get(), type.get());
    }

    /**
     * Runs {@code fitting} with upgrade {@code upgradeId} of {@code type} if {@code structure} can take it, cost
     * aside, or gives {@code refused} the reason it cannot.
     */
    <T> @NotNull T upgrade(@NotNull PlacedStructure structure, @NotNull StructureType type, @NotNull String upgradeId,
                           @NotNull Function<Component, T> refused, @NotNull Function<StructureUpgrade, T> fitting) {
        final Optional<StructureUpgrade> found = type.upgrade(upgradeId);
        if (found.isEmpty()) {
            return refused.apply(text("core.construction.unknown_upgrade"));
        }
        final StructureUpgrade upgrade = found.get();
        if (structure.getStage() < upgrade.getStage()) {
            return refused.apply(text("core.construction.upgrade_locked"));
        }
        if (structure.upgradeAt(upgrade.getStage()).isPresent()) {
            return refused.apply(text("core.construction.upgrade_taken"));
        }
        if (structure.getJob() != null) {
            return refused.apply(text("core.construction.busy"));
        }
        if (structure.getCondition() != StructureCondition.ACTIVE) {
            return refused.apply(text("core.construction.upgrade_needs_active"));
        }
        return fitting.apply(upgrade);
    }

    @NotNull Optional<Component> allowed(@NotNull Player player, @NotNull SiteHolding at,
                                         @NotNull ConstructionAction action) {
        if (at.getSite().allows(player, at.getKey(), action)) {
            return Optional.empty();
        }
        return Optional.of(text(action == ConstructionAction.BUILD
                ? "core.construction.build_not_allowed" : "core.construction.action_not_allowed"));
    }

    /** The build checks at {@code worksite} with {@code position}, cost aside. */
    @NotNull Optional<Component> buildProblem(@NotNull Player player, @NotNull Worksite worksite,
                                              @NotNull StructureType type, @NotNull StructurePosition position) {
        return buildProblem(player, worksite.siteHolding(), type, () -> fit(worksite, type, 0, position, null));
    }

    private @NotNull Optional<Component> buildProblem(@NotNull Player player, @NotNull SiteHolding at,
                                                      @NotNull StructureType type,
                                                      @NotNull Supplier<Optional<Component>> fit) {
        return allowed(player, at, ConstructionAction.BUILD)
                .or(() -> requirements(at, type, 0))
                .or(fit);
    }

    /** The move checks, cost aside. */
    @NotNull Optional<Component> moveProblem(@NotNull Worksite worksite, @NotNull PlacedStructure structure,
                                             @NotNull StructureType type, @NotNull StructurePosition target) {
        if (!type.getFlags().isMovable()) {
            return Optional.of(text("core.construction.not_movable"));
        }
        if (structure.getJob() != null) {
            return Optional.of(text("core.construction.busy"));
        }
        return fit(worksite, type, structure.getStage(), target, structure.getId());
    }

    @NotNull Optional<Component> requirements(@NotNull SiteHolding at, @NotNull StructureType type, int stage) {
        for (String required : type.getRequiredStructures()) {
            if (!at.getHolding().hasBuilt(required)) {
                final Component name = catalogue.find(required).map(StructureType::getDisplayName)
                        .orElse(Component.text(required));
                return Optional.of(text("core.construction.requires", name));
            }
        }
        return at.getSite().blocked(at.getKey(), at.getHolding(), type, stage);
    }

    @NotNull Optional<Component> fit(@NotNull Worksite worksite, @NotNull StructureType type, int stage,
                                     @NotNull StructurePosition position, @Nullable UUID ignoring) {
        final Optional<SchematicPlacement> placement = shapes.placementOf(worksite.getWorld(), type.getId(), stage,
                position);
        if (placement.isEmpty()) {
            return Optional.of(text("core.construction.no_build"));
        }
        return fitCheck.problem(worksite.getWorld(), worksite.getHolding(), type, placement.get(), ignoring);
    }

    @NotNull Optional<Component> afford(@NotNull SiteHolding at, @NotNull ResourceCost cost) {
        return at.getSite().ledger().canAfford(at.getKey(), cost)
                ? Optional.empty() : Optional.of(text("core.construction.cannot_afford"));
    }

    private static @NotNull Component text(@NotNull String key, @NotNull ComponentLike... args) {
        return Translations.component(key, args).color(NamedTextColor.RED);
    }
}
