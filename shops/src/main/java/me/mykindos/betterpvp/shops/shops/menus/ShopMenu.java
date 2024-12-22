package me.mykindos.betterpvp.shops.shops.menus;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.shops.shops.menus.buttons.ShopItemButton;

import java.util.List;

@CustomLog
public class ShopMenu extends AbstractGui {

    public ShopMenu(int page, List<IShopItem> shopItems, ItemHandler itemHandler, ClientManager clientManager) {
        super(9, 6);

        for (IShopItem shopItem : shopItems) {
            if (shopItem.getPage() != page) continue;
            if (shopItem.getSlot() > 47 && shopItem.getSlot() < 51) {
                log.warn("{} in slot {} will not be shown, cannot have shop items in this slot",
                        shopItem.getItemName(), shopItem.getSlot()).submit();
            }

            if (shopItem.getSlot() > 50) {
                shopItem.setSlot(shopItem.getSlot() - 3);
            }

            setItem(shopItem.getSlot(), new ShopItemButton(shopItem, itemHandler, clientManager));

        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

}
