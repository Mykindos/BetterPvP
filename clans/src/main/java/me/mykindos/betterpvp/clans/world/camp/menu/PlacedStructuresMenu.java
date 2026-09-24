package me.mykindos.betterpvp.clans.world.camp.menu;

import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.clans.world.camp.upgrade.BuildQueue;
import me.mykindos.betterpvp.clans.world.camp.upgrade.QueuedAction;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Every structure a camp has, with its stage and what it is doing. Clicking one opens what can be done to it. */
public class PlacedStructuresMenu extends AbstractGui implements Windowed {

    PlacedStructuresMenu(@NotNull StructureMenus menus, @NotNull SiteKey camp, @Nullable Windowed previous) {
        super(9, 6);
        final long now = menus.getConstruction().now();
        final List<PlacedStructure> structures = menus.holding(camp).map(Holding::getStructures)
                .map(List::copyOf)
                .orElse(List.of())
                .stream()
                .filter(structure -> menus.type(structure.getType()).isPresent())
                .toList();

        if (structures.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.menu.structures.none").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int slot = 0; slot < structures.size() && slot < 45; slot++) {
            final PlacedStructure structure = structures.get(slot);
            final CampStructure type = menus.type(structure.getType()).orElseThrow();
            setItem(slot, new SimpleItem(summary(type, structure, now)
                    .lore(Component.empty())
                    .action(ClickActions.ALL, Translations.component("clans.camp.menu.structures.manage"))
                    .build(), click -> new StructureActionsMenu(menus, click.getPlayer(), camp, structure.getId(),
                    previous, null).show(click.getPlayer())));
        }
        final BuildQueue queue = menus.getBuildQueue();
        final Optional<QueuedAction> queued = queue.queued(camp);
        if (queue.isActive(camp) || queued.isPresent()) {
            setItem(45, queued(menus, camp, previous, queued.orElse(null)));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    /** What the camp has queued, which a member who could take that action, or who queued it, can clear. */
    private static @NotNull SimpleItem queued(@NotNull StructureMenus menus, @NotNull SiteKey camp,
                                              @Nullable Windowed previous, @Nullable QueuedAction queued) {
        final BuildQueue queue = menus.getBuildQueue();
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.PAPER)
                .displayName(Translations.component("clans.camp.upgrade.build_queue.menu")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true);
        if (queued == null) {
            view.lore(Translations.component("clans.camp.upgrade.build_queue.menu.empty").color(NamedTextColor.GRAY));
            return new SimpleItem(view.build());
        }
        view.lore(queue.describe(queued).color(NamedTextColor.WHITE))
                .lore(Translations.component("clans.camp.upgrade.build_queue.button.description")
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.build_queue.menu.clear"));
        return new SimpleItem(view.build(), click -> {
            final Player player = click.getPlayer();
            final boolean mayClear = player.getUniqueId().equals(queued.getQueuedBy())
                    || menus.getPermissions().allows(player, camp.getOwnerId(), queued.getAction());
            if (!mayClear) {
                menus.tell(player, Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
                return;
            }
            if (queue.clear(camp) != null) {
                menus.tell(player, Translations.component("clans.camp.upgrade.build_queue.cleared",
                        queue.describe(queued)).color(NamedTextColor.GRAY));
            }
            new PlacedStructuresMenu(menus, camp, previous).show(player);
        });
    }

    /** A structure's icon, name, stage and status, with how long its job has left if one is running. */
    static @NotNull ItemView.ItemViewBuilder summary(@NotNull CampStructure type, @NotNull PlacedStructure structure,
                                                     long now) {
        final StructureStatus status = structure.status(now);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(type.getIcon())
                .displayName(type.getDisplayName().color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(type.stageName(structure.getStage()).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.menu.structures.status",
                        Translations.component("clans.camp.menu.structures.status." + status.name().toLowerCase(Locale.ROOT))
                                .color(color(status))).color(NamedTextColor.GRAY));

        final Job job = structure.getJob();
        final Optional<Long> remaining = Optional.ofNullable(job)
                .filter(running -> !running.isHeld() && !running.isDone(now))
                .map(running -> running.remainingMillis(now))
                .filter(millis -> millis != Long.MAX_VALUE);
        remaining.ifPresent(millis -> view.lore(Translations.component("clans.camp.menu.structures.time_left",
                Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(1000, millis)))).color(NamedTextColor.WHITE))
                .color(NamedTextColor.GRAY)));
        return view;
    }

    private static @NotNull NamedTextColor color(@NotNull StructureStatus status) {
        return switch (status) {
            case ACTIVE -> NamedTextColor.GREEN;
            case READY_TO_CLAIM -> NamedTextColor.GOLD;
            case DISABLED, NEEDS_REPAIR -> NamedTextColor.RED;
            case PAUSED, NOT_PLACED -> NamedTextColor.DARK_GRAY;
            default -> NamedTextColor.YELLOW;
        };
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.menu.structures.title");
    }
}
