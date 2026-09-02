package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The maximum health the selected class equips with, read out beside the class grid.
 */
public class ClassHealthButton extends ControlItem<ClassSelectionMenu> {

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Purely a readout
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        final Role role = gui.getSelected();

        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/shadowed/heart_icon"))
                .displayName(Translations.component("champions.menu.class.health").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(UtilFormat.formatNumber(role.getHealth()), NamedTextColor.RED))
                        .append(Component.space())
                        .append(Component.text('❤', NamedTextColor.RED)))
                .hideAdditionalTooltip(true)
                .build();
    }
}
