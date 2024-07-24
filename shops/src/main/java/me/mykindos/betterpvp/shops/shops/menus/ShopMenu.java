package me.mykindos.betterpvp.shops.shops.menus;

import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.shops.shops.menus.buttons.ShopItemButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShopMenu extends AbstractGui implements Windowed {

    private final Component title;

    public ShopMenu(Component title, List<IShopItem> shopItems, ItemHandler itemHandler, ClientManager clientManager) {
        super(9, 6);
        this.title = title;

        for (IShopItem shopItem : shopItems) {
            setItem(shopItem.getSlot(), new ShopItemButton(shopItem, itemHandler, clientManager));
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return title;
    }

}
