package me.mykindos.betterpvp.clans.world.camp.menu;

import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.clans.world.camp.upgrade.BuildQueue;
import me.mykindos.betterpvp.clans.world.camp.upgrade.QueuedAction;
import me.mykindos.betterpvp.clans.world.camp.upgrade.RushOrder;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * What can be done to one placed structure in its current state, each with what it costs and how long it takes. An
 * action that cannot be taken says why. Cancelling and demolishing lose something, so each takes a second click.
 * <p>
 * Acting needs the viewer to stand in the camp, because every action works on the camp's loaded world.
 */
public class StructureActionsMenu extends AbstractGui implements Windowed {

    private final StructureMenus menus;
    private final Player viewer;
    private final SiteKey camp;
    private final UUID id;
    private final @Nullable Windowed back;
    private final @Nullable Windowed returnTo;
    private final @Nullable ConstructionService.Worksite worksite;
    private Component title = Component.empty();

    /**
     * @param back       where the structure list this menu returns to leads back to
     * @param confirming the action waiting on its confirming click, if any
     */
    StructureActionsMenu(@NotNull StructureMenus menus, @NotNull Player viewer, @NotNull SiteKey camp, @NotNull UUID id,
                         @Nullable Windowed back, @Nullable ConstructionAction confirming) {
        this(menus, viewer, camp, id, back, confirming, null);
    }

    /** @param returnTo where Back goes instead of the structure list, or null for the list */
    StructureActionsMenu(@NotNull StructureMenus menus, @NotNull Player viewer, @NotNull SiteKey camp, @NotNull UUID id,
                         @Nullable Windowed back, @Nullable ConstructionAction confirming,
                         @Nullable Windowed returnTo) {
        super(9, 4);
        this.menus = menus;
        this.viewer = viewer;
        this.camp = camp;
        this.id = id;
        this.back = back;
        this.returnTo = returnTo;
        this.worksite = menus.worksite(viewer, camp).orElse(null);

        final Optional<PlacedStructure> found = menus.holding(camp).flatMap(holding -> holding.find(id));
        final Optional<CampStructure> type = found.flatMap(structure -> menus.type(structure.getType()));
        if (found.isPresent() && type.isPresent()) {
            title = type.get().getDisplayName();
            populate(found.get(), type.get(), confirming);
        } else {
            setItem(13, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("core.construction.missing_structure").color(NamedTextColor.RED))
                    .build()));
        }
        setItem(31, new BackButton(returnTo != null ? returnTo : new PlacedStructuresMenu(menus, camp, back)));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private void populate(@NotNull PlacedStructure structure, @NotNull CampStructure type,
                          @Nullable ConstructionAction confirming) {
        final long now = menus.getConstruction().now();
        final StructureStatus status = structure.status(now);
        final Job job = structure.getJob();
        final boolean busy = job != null;
        setItem(4, new SimpleItem(PlacedStructuresMenu.summary(type, structure, now).build()));

        final List<SimpleItem> buttons = new ArrayList<>();
        if (status == StructureStatus.READY_TO_CLAIM) {
            buttons.add(button(Material.EMERALD, ConstructionAction.CLAIM, null,
                    Translations.component("clans.camp.menu.structures.action.claim"),
                    List.of(Translations.component("clans.camp.menu.structures.action.claim.description")),
                    null, "clans.camp.menu.structures.done.claim", type,
                    (service, world) -> service.claim(viewer, world, id)));
        }

        final int next = structure.getStage() + 1;
        if (type.hasStage(next)) {
            final StructureStage stage = type.stage(next);
            final Component problem = busy || structure.getCondition() != StructureCondition.ACTIVE
                    ? Translations.component("core.construction.advance_needs_idle") : unaffordable(stage.getCost());
            buttons.add(button(Material.EXPERIENCE_BOTTLE, ConstructionAction.ADVANCE, null,
                    Translations.component("clans.camp.menu.structures.action.advance", type.stageName(next)),
                    List.of(costLine(stage.getCost()), timeLine(stage.getBuildTime())),
                    problem, "clans.camp.menu.structures.done.advance", type,
                    (service, world) -> service.advance(viewer, world, id)));
        }

        final StructureCondition condition = structure.getCondition();
        if (condition == StructureCondition.DISABLED || condition == StructureCondition.NEEDS_REPAIR) {
            final Component problem = busy ? Translations.component("core.construction.already_working")
                    : unaffordable(type.getRepairCost());
            buttons.add(button(Material.ANVIL, ConstructionAction.REPAIR, null,
                    Translations.component("clans.camp.menu.structures.action.repair"),
                    List.of(costLine(type.getRepairCost()), timeLine(type.getRepairTime())),
                    problem, "clans.camp.menu.structures.done.repair", type,
                    (service, world) -> service.repair(viewer, world, id)));
        }

        final BuildQueue queue = menus.getBuildQueue();
        final Optional<ConstructionAction> queueable = BuildQueue.queueable(structure, type);
        final boolean busyElsewhere = menus.holding(camp)
                .map(holding -> BuildQueue.busyElsewhere(holding, id))
                .orElse(false);
        if (queue.isActive(camp) && queueable.isPresent() && busyElsewhere) {
            buttons.add(queue(structure, type, queueable.get()));
        }

        final RushOrder rush = menus.getRushOrder();
        if (job != null && !job.isDone(now) && rush.isActive(camp)) {
            buttons.add(rush(structure, type, rush.price(job, now)));
        }

        if (type.getFlags().isMovable()) {
            buttons.add(move(type, busy ? Translations.component("core.construction.busy")
                    : unaffordable(type.getMoveCost())));
        }

        if (job != null) {
            buttons.add(button(Material.BARRIER, ConstructionAction.CANCEL, confirming,
                    Translations.component("clans.camp.menu.structures.action.cancel"),
                    List.of(refundLine(job.getSpent()),
                            Translations.component("clans.camp.menu.structures.action.cancel.warning")),
                    null, "clans.camp.menu.structures.done.cancel", type,
                    (service, world) -> service.cancel(viewer, world, id)));
        }

        if (type.getFlags().isDemolishable()) {
            final Component problem = condition == StructureCondition.UNDER_CONSTRUCTION
                    || condition == StructureCondition.NOT_PLACED
                    ? Translations.component("core.construction.demolish_needs_standing")
                    : busy ? Translations.component("core.construction.busy") : null;
            final ResourceCost refund = menus.getConstruction().demolishRefund(camp, structure, type);
            buttons.add(button(Material.TNT, ConstructionAction.DEMOLISH, confirming,
                    Translations.component("clans.camp.menu.structures.action.demolish"),
                    List.of(refundLine(refund),
                            Translations.component("clans.camp.menu.structures.action.demolish.warning")),
                    problem, "clans.camp.menu.structures.done.demolish", type,
                    (service, world) -> service.demolish(viewer, world, id)));
        }

        final int first = 13 - (buttons.size() - 1) / 2;
        for (int i = 0; i < buttons.size(); i++) {
            setItem(first + i, buttons.get(i));
        }
    }

    /**
     * One action. It is refused before it is tried if the viewer is not in the camp, their rank may not take it, or
     * {@code problem} says why the structure cannot have it. With {@code confirming} not yet {@code action}, the first
     * click only asks for a second.
     */
    private @NotNull SimpleItem button(@NotNull Material icon, @NotNull ConstructionAction action,
                                       @Nullable ConstructionAction confirming, @NotNull Component name,
                                       @NotNull List<Component> details, @Nullable Component problem,
                                       @NotNull String done, @NotNull CampStructure type,
                                       @NotNull BiFunction<ConstructionService, World, ConstructionResult> run) {
        final Component blocked = blocked(action, problem);
        final boolean needsConfirm = action == ConstructionAction.CANCEL || action == ConstructionAction.DEMOLISH;
        final boolean armed = needsConfirm && confirming == action;
        final ItemView.ItemViewBuilder view = view(icon, name, details, blocked).glow(armed);
        if (blocked == null) {
            view.action(ClickActions.ALL, armed
                    ? Translations.component("clans.camp.menu.structures.confirm")
                    : Translations.component("clans.camp.menu.structures.act"));
        }

        return new SimpleItem(view.build(), click -> {
            final Player player = click.getPlayer();
            if (blocked != null || worksite == null) {
                return;
            }
            if (needsConfirm && !armed) {
                new StructureActionsMenu(menus, player, camp, id, back, action, returnTo).show(player);
                return;
            }
            final ConstructionResult result = run.apply(menus.getConstruction(), worksite.getWorld());
            if (!result.isSuccess()) {
                if (result.getReason() != null) {
                    menus.tell(player, result.getReason());
                }
            } else {
                menus.tell(player, Translations.component(done, type.getDisplayName()).color(NamedTextColor.GRAY));
            }
            reopen(player);
        });
    }

    /** Queues {@code action} to start once the camp's next job is claimed, instead of starting it now. */
    private @NotNull SimpleItem queue(@NotNull PlacedStructure structure, @NotNull CampStructure type,
                                      @NotNull ConstructionAction action) {
        final BuildQueue queue = menus.getBuildQueue();
        final Optional<QueuedAction> waiting = queue.queued(camp);
        final Component blocked;
        if (!menus.getPermissions().allows(viewer, camp.getOwnerId(), action)) {
            blocked = Translations.component("clans.settler.card.not_allowed");
        } else {
            blocked = waiting.map(queued -> Translations.component("clans.camp.upgrade.build_queue.full",
                    queue.describe(queued))).orElse(null);
        }
        final QueuedAction preview = new QueuedAction(id, type.getId(), action, viewer.getUniqueId());
        final ItemView.ItemViewBuilder view = view(Material.PAPER,
                Translations.component("clans.camp.upgrade.build_queue.button", queue.describe(preview)),
                List.of(Translations.component("clans.camp.upgrade.build_queue.button.description")), blocked);
        if (blocked == null) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.menu.structures.act"));
        }
        return new SimpleItem(view.build(), click -> {
            if (blocked != null) {
                return;
            }
            final Player player = click.getPlayer();
            if (queue.queue(player, camp, structure, action)) {
                menus.tell(player, Translations.component("clans.camp.upgrade.build_queue.queued",
                        queue.describe(preview)).color(NamedTextColor.GRAY));
            }
            reopen(player);
        });
    }

    /** Pays coins from the viewer's own balance to finish the running job at once. */
    private @NotNull SimpleItem rush(@NotNull PlacedStructure structure, @NotNull CampStructure type, long price) {
        final RushOrder rush = menus.getRushOrder();
        final Component blocked = worksite == null
                ? Translations.component("clans.camp.menu.structures.not_in_camp")
                : rush.problem(viewer, camp, structure).orElse(null);
        final ItemView.ItemViewBuilder view = view(Material.CLOCK,
                Translations.component("clans.camp.upgrade.rush_order.button"),
                List.of(Translations.component("clans.camp.upgrade.rush_order.price",
                                Component.text(UtilFormat.formatNumber((int) price), NamedTextColor.GOLD)),
                        Translations.component("clans.camp.upgrade.rush_order.button.description")), blocked);
        if (blocked == null) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.menu.structures.act"));
        }
        return new SimpleItem(view.build(), click -> {
            final Player player = click.getPlayer();
            if (blocked != null || worksite == null) {
                return;
            }
            final ConstructionResult result = rush.rush(player, worksite, structure);
            if (!result.isSuccess()) {
                if (result.getReason() != null) {
                    menus.tell(player, result.getReason());
                }
            } else {
                menus.tell(player, Translations.component("clans.camp.upgrade.rush_order.done",
                        type.getDisplayName()).color(NamedTextColor.GRAY));
            }
            reopen(player);
        });
    }

    /** Moving hands over a plan bound to this structure, and placing that plan moves it. */
    private @NotNull SimpleItem move(@NotNull CampStructure type, @Nullable Component problem) {
        final Component blocked = blocked(ConstructionAction.MOVE, problem);
        final List<Component> details = List.of(costLine(type.getMoveCost()), timeLine(type.getMoveTime()),
                Translations.component("clans.camp.menu.structures.action.move.description"));
        final ItemView.ItemViewBuilder view = view(Material.COMPASS,
                Translations.component("clans.camp.menu.structures.action.move"), details, blocked);
        if (blocked == null) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.menu.structures.act"));
        }
        return new SimpleItem(view.build(), click -> {
            if (blocked != null) {
                return;
            }
            final Player player = click.getPlayer();
            player.getInventory().addItem(menus.getBlueprints().blueprintToMove(type, id));
            menus.tell(player, Translations.component("clans.camp.menu.structures.action.move.given",
                    type.getDisplayName()).color(NamedTextColor.GRAY));
            player.closeInventory();
        });
    }

    private @Nullable Component blocked(@NotNull ConstructionAction action, @Nullable Component problem) {
        if (worksite == null) {
            return Translations.component("clans.camp.menu.structures.not_in_camp");
        }
        if (!menus.getPermissions().allows(viewer, camp.getOwnerId(), action)) {
            return Translations.component("clans.settler.card.not_allowed");
        }
        return problem;
    }

    private @Nullable Component unaffordable(@NotNull ResourceCost cost) {
        return menus.getResources().canAfford(camp, cost) ? null
                : Translations.component("core.construction.cannot_afford");
    }

    private static @NotNull ItemView.ItemViewBuilder view(@NotNull Material icon, @NotNull Component name,
                                                          @NotNull List<Component> details,
                                                          @Nullable Component blocked) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(icon)
                .displayName(name.color(blocked == null ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                        .decorate(TextDecoration.BOLD))
                .frameLore(true);
        details.forEach(line -> view.lore(line.colorIfAbsent(NamedTextColor.GRAY)));
        if (blocked != null) {
            view.lore(Component.empty());
            view.lore(blocked.color(NamedTextColor.RED));
        }
        return view;
    }

    private void reopen(@NotNull Player player) {
        final boolean stands = menus.holding(camp).flatMap(holding -> holding.find(id)).isPresent();
        if (stands) {
            new StructureActionsMenu(menus, player, camp, id, back, null, returnTo).show(player);
        } else if (returnTo != null) {
            returnTo.show(player);
        } else {
            menus.openList(player, camp, back);
        }
    }

    private static @NotNull Component costLine(@NotNull ResourceCost cost) {
        return Translations.component("clans.camp.menu.build.cost", ConstructionMenu.cost(cost));
    }

    private static @NotNull Component refundLine(@NotNull ResourceCost cost) {
        return Translations.component("clans.camp.menu.structures.refund", ConstructionMenu.cost(cost));
    }

    private static @NotNull Component timeLine(@NotNull Duration time) {
        return Translations.component("clans.camp.menu.structures.time", time.isZero()
                ? Translations.component("clans.camp.menu.build.instant").color(NamedTextColor.WHITE)
                : Component.text(UtilTime.humanReadableFormat(time), NamedTextColor.WHITE));
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }
}
