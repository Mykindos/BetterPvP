package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.ShopItemMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SelectedShopItemButton extends ControlItem<ShopItemMenu> {

    private final ShopItemMenu itemMenu;

    @Override
    public ItemProvider getItemProvider(ShopItemMenu gui) {
        final ItemView view = itemMenu.getContext().createDisplayStack(itemMenu.getShopItem(), itemMenu.getAmount());
        final ItemView.ItemViewBuilder builder = view.toBuilder();

        builder.displayName(Component.empty()
                .append(view.toItemStack().effectiveName())
                .appendSpace()
                .append(Component.text("x" + itemMenu.getAmount(), NamedTextColor.YELLOW, TextDecoration.BOLD)));

        builder.amount(itemMenu.getAmount());
        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
    }
}
