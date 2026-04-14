package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import me.mykindos.betterpvp.shops.shops.menus.SortMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SortButton extends AbstractItem {

    private final ShopMenu menu;

    @Override
    public ItemProvider getItemProvider() {
        boolean titled = false;

        ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE);

        for (SortMode sortMode : SortMode.values()) {
            TextColor color = sortMode == menu.getSortMode() ? TextColor.color(0xFFD700) : NamedTextColor.GRAY;
            if (!titled) {
                builder.displayName(Component.text(sortMode.getDisplayName(), color));
                titled = true;
            } else {
                builder.lore(Component.text(sortMode.getDisplayName(), color));
            }
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            menu.setSortMode(menu.getSortMode().next());
        } else if (clickType.isRightClick()) {
            menu.setSortMode(menu.getSortMode().previous());
        }
        menu.refresh();
        notifyWindows();
    }
}
