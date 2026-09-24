package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One structure's upgrades, a row for each stage: the stage, then the upgrades it offers. A stage takes one of them,
 * and the rest of its row greys out once one is chosen.
 */
public class StructureUpgradesMenu extends AbstractGui implements Windowed {

    private final HallMenus menus;
    private final SiteKey key;
    private final PlacedStructure structure;
    private final CampStructure type;
    private final @Nullable Windowed previous;

    StructureUpgradesMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key,
                          @NotNull PlacedStructure structure, @NotNull CampStructure type,
                          @Nullable Windowed previous) {
        super(9, 6);
        this.menus = menus;
        this.key = key;
        this.structure = structure;
        this.type = type;
        this.previous = previous;

        final List<StructureUpgrade> upgrades = type.getUpgrades();
        for (int stage = 0; stage < type.getStages().size() && stage < 5; stage++) {
            final int row = stage * 9;
            setItem(row, stageItem(stage));
            int column = 2;
            for (StructureUpgrade upgrade : upgrades) {
                if (upgrade.getStage() == stage && column < 9) {
                    setItem(row + column++, upgradeItem(viewer, upgrade));
                }
            }
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem stageItem(int stage) {
        final Optional<String> picked = structure.upgradeAt(stage);
        final Component state;
        if (structure.getStage() < stage) {
            state = Translations.component("clans.camp.upgrade.menu.stage_locked").color(NamedTextColor.RED);
        } else if (picked.isPresent()) {
            state = Translations.component("clans.camp.upgrade.menu.stage_chosen", upgradeName(picked.get()))
                    .color(NamedTextColor.GREEN);
        } else {
            state = Translations.component("clans.camp.upgrade.menu.stage_open").color(NamedTextColor.YELLOW);
        }
        return new SimpleItem(ItemView.builder()
                .material(structure.getStage() < stage ? Material.GRAY_DYE : type.getIcon())
                .displayName(type.stageName(stage).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(state)
                .build());
    }

    private @NotNull SimpleItem upgradeItem(@NotNull Player viewer, @NotNull StructureUpgrade upgrade) {
        final boolean chosen = structure.hasUpgrade(upgrade.getId());
        final Job job = structure.getJob();
        final boolean fitting = job != null && job.getKind() == JobKind.FIT_UPGRADE
                && upgrade.getId().equals(job.getUpgrade());
        final Optional<Component> unavailable = chosen || fitting ? Optional.empty()
                : menus.getConstruction().upgradeUnavailable(viewer, key, structure.getId(), upgrade.getId());
        final boolean greyed = !chosen && !fitting && unavailable.isPresent();

        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(greyed ? Material.GRAY_STAINED_GLASS_PANE : type.upgradeIcon(upgrade.getId()))
                .displayName(upgradeName(upgrade.getId())
                        .color(chosen ? NamedTextColor.GREEN : greyed ? NamedTextColor.GRAY : NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .glow(chosen)
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade." + upgrade.getId() + ".description")
                        .color(NamedTextColor.GRAY));

        if (!chosen) {
            view.lore(Component.empty());
            view.lore(Translations.component("clans.camp.upgrade.menu.cost", cost(upgrade.getCost()))
                    .color(NamedTextColor.GRAY));
            view.lore(Translations.component("clans.camp.upgrade.menu.time", time(upgrade.getTime()))
                    .color(NamedTextColor.GRAY));
            if (upgrade.getWorkforce() > 0) {
                view.lore(Translations.component("clans.camp.upgrade.menu.workforce",
                        Component.text(upgrade.getWorkforce())).color(NamedTextColor.GRAY));
            }
        }

        view.lore(Component.empty());
        if (chosen) {
            view.lore(Translations.component("clans.camp.upgrade.menu.chosen").color(NamedTextColor.GREEN));
        } else if (fitting) {
            view.lore(Translations.component("clans.camp.upgrade.menu.fitting",
                    time(Duration.ofMillis(job.remainingMillis(menus.getConstruction().now()))))
                    .color(NamedTextColor.YELLOW));
        } else if (unavailable.isPresent()) {
            view.lore(unavailable.get());
        } else {
            view.action(ClickActions.ALL, Translations.component("clans.camp.upgrade.menu.action"));
        }

        return new SimpleItem(view.build(), click -> {
            if (!chosen && !fitting && unavailable.isEmpty()) {
                fit(click.getPlayer(), upgrade);
            }
        });
    }

    private void fit(@NotNull Player player, @NotNull StructureUpgrade upgrade) {
        final ConstructionResult result = menus.getConstruction().upgrade(player, player.getWorld(),
                structure.getId(), upgrade.getId());
        if (!result.isSuccess()) {
            if (result.getReason() != null) {
                menus.tell(player, result.getReason());
            }
            return;
        }
        menus.tell(player, structure.hasUpgrade(upgrade.getId())
                ? "clans.camp.upgrade.menu.fitted" : "clans.camp.upgrade.menu.started", upgradeName(upgrade.getId()));
        new StructureUpgradesMenu(menus, player, key, structure, type, previous).show(player);
    }

    private static @NotNull Component upgradeName(@NotNull String upgrade) {
        return Translations.component("clans.camp.upgrade." + upgrade + ".name");
    }

    private static @NotNull Component time(@NotNull Duration time) {
        return time.isZero() ? Translations.component("clans.camp.menu.build.instant")
                : Component.text(UtilTime.humanReadableFormat(time));
    }

    private static @NotNull Component cost(@NotNull ResourceCost cost) {
        if (cost.isFree()) {
            return Translations.component("clans.camp.menu.build.free");
        }
        final List<Component> parts = new ArrayList<>();
        cost.getAmounts().forEach((resource, amount) -> ResourceKind.byId(resource).ifPresent(kind ->
                parts.add(Translations.component("clans.camp.resource.amount", Component.text(amount), kind.displayName()))));
        return Component.join(JoinConfiguration.commas(true), parts);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.menu.structure_title", type.getDisplayName());
    }
}
