package me.mykindos.betterpvp.shops.shops.menus.buttons;

import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ShopItemButton extends Button {

    private final IShopItem shopItem;

    private final ItemStack item;

    public ShopItemButton(int slot, IShopItem shopItem, ItemStack item) {
        super(slot, item);
        this.shopItem = shopItem;
        this.item = item;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            UtilServer.callEvent(new PlayerBuyItemEvent(player, shopItem, item, clickType));
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            UtilServer.callEvent(new PlayerSellItemEvent(player, shopItem, item, clickType));
        }
    }


}
