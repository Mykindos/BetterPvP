package me.mykindos.betterpvp.shops.shops.menus;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.shops.shops.menus.buttons.ChangeAmountButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SelectedShopItemButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SetAmountButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.TransactionButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.direction.BackToPreviousButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.direction.DisabledPageButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@Getter
public class ShopItemMenu extends AbstractGui implements Windowed {

    private final ShopContext context;
    private final IShopItem shopItem;
    private final Windowed previous;
    @Setter
    private int amount = 1;

    public ShopItemMenu(ShopContext context, IShopItem shopItem, Windowed previous) {
        super(9, 6);
        this.context = context;
        this.shopItem = shopItem;
        this.previous = previous;
        setItem(13, new SelectedShopItemButton(this));
        setItem(10, new ChangeAmountButton(this, -1));
        setItem(11, new ChangeAmountButton(this, -3));
        setItem(12, new ChangeAmountButton(this, -5));
        setItem(14, new ChangeAmountButton(this, 1));
        setItem(15, new ChangeAmountButton(this, 3));
        setItem(16, new ChangeAmountButton(this, 5));
        setItem(20, new SetAmountButton(this, 1));
        setItem(22, new SetAmountButton(this, 32));
        setItem(24, new SetAmountButton(this, 64));
        setItem(29, new TransactionButton(this, true));
        setItem(30, new TransactionButton(this, true));
        setItem(32, new TransactionButton(this, false));
        setItem(33, new TransactionButton(this, false));
        setItem(47, new BackToPreviousButton(previous, false));
        setItem(48, new BackToPreviousButton(previous, true));
        setItem(50, new DisabledPageButton(true, false));
        setItem(51, new DisabledPageButton(true, true));
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-13><glyph:menu_shop_buy_sell>").font(NEXO);
    }

    public void notifyOpenWindows() {
        this.updateControlItems();
    }

    public boolean isViewingShopItem(IShopItem target) {
        return shopItem.getId() == target.getId();
    }
}
