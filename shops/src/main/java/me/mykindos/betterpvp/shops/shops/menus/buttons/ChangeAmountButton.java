package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.ShopItemMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class ChangeAmountButton extends ControlItem<ShopItemMenu> {

    private final ShopItemMenu itemMenu;
    private final int change;

    @Override
    public ItemProvider getItemProvider(ShopItemMenu menu) {
        int newAmount = calculateChange();
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Component.empty()
                        .append(Component.text("Set to", NamedTextColor.GRAY))
                        .appendSpace()
                        .append(Component.text(newAmount, change > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .build();
    }

    private int calculateChange() {
        return Math.max(1, Math.min(64, itemMenu.getAmount() + change));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        itemMenu.setAmount(calculateChange());
        itemMenu.notifyOpenWindows();
    }
}
