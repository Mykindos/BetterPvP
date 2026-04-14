package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.SellAllMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SellStagedItemsButton extends ControlItem<SellAllMenu> {

    private final SellAllMenu sellAllMenu;

    @Override
    public ItemProvider getItemProvider(SellAllMenu gui) {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Component.text("Sell for ", TextColor.color(144, 238, 144))
                        .append(UtilMessage.deserialize("<gold>" + UtilFormat.formatNumber(sellAllMenu.getTotalPrice()) + "g <coins></coins></gold>")))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        sellAllMenu.sellAll(player);
        player.closeInventory();
    }
}
