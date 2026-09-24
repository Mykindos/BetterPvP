package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** The camp's structures that offer upgrades, with how many stages still have a pick waiting. */
public class UpgradesMenu extends AbstractGui implements Windowed {

    UpgradesMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key, @Nullable Windowed previous) {
        super(9, 4);
        final List<PlacedStructure> placed = menus.getStore().cached(key.getOwnerId())
                .map(Camp::getHolding)
                .map(holding -> holding.getStructures().stream()
                        .filter(structure -> structure.getCondition() != StructureCondition.NOT_PLACED
                                && structure.getCondition() != StructureCondition.UNDER_CONSTRUCTION)
                        .toList())
                .orElse(List.of());

        int slot = 10;
        for (PlacedStructure structure : placed) {
            final Optional<CampStructure> type = menus.getStructures().find(structure.getType())
                    .filter(found -> !found.getUpgrades().isEmpty());
            if (type.isEmpty() || slot > 25) {
                continue;
            }
            setItem(slot, entry(menus, key, structure, type.get()));
            slot += slot % 9 == 7 ? 3 : 1;
        }
        if (slot == 10) {
            setItem(13, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.upgrade.menu.none").color(NamedTextColor.GRAY))
                    .build()));
        }
        setItem(31, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem entry(@NotNull HallMenus menus, @NotNull SiteKey key, @NotNull PlacedStructure structure,
                                      @NotNull CampStructure type) {
        int waiting = 0;
        for (int stage = 0; stage <= structure.getStage(); stage++) {
            final int reached = stage;
            final boolean offers = type.getUpgrades().stream().map(StructureUpgrade::getStage)
                    .anyMatch(offered -> offered == reached);
            if (offers && structure.upgradeAt(stage).isEmpty()) {
                waiting++;
            }
        }

        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(type.getIcon())
                .displayName(type.getDisplayName().color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(type.stageName(structure.getStage()).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.upgrade.menu.chosen_count",
                        Component.text(structure.getUpgrades().size())).color(NamedTextColor.GRAY));
        if (waiting > 0) {
            view.lore(Translations.component("clans.camp.upgrade.menu.waiting", Component.text(waiting))
                    .color(NamedTextColor.GREEN));
        }
        view.action(ClickActions.ALL, Translations.component("clans.camp.hall.open"));
        return new SimpleItem(view.build(), click ->
                new StructureUpgradesMenu(menus, click.getPlayer(), key, structure, type, this).show(click.getPlayer()));
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.menu.title");
    }
}
