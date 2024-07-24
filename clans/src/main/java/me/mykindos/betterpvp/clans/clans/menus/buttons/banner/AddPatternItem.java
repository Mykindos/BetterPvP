package me.mykindos.betterpvp.clans.clans.menus.buttons.banner;

import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.impl.GuiSelectPattern;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class AddPatternItem extends ControlItem<BannerMenu> {

    @Override
    public ItemProvider getItemProvider(BannerMenu gui) {
        return ItemView.builder()
                .material(Material.FLOWER_BANNER_PATTERN)
                .displayName(Component.text("Add Pattern", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        GuiSelectPattern guiSelectPattern = new GuiSelectPattern(pattern -> {
            getGui().getBuilder().pattern(pattern);
            getGui().update(); // Update the GUI to reflect changes
            getGui().show(player);
        });
        guiSelectPattern.show(player);
    }
}
