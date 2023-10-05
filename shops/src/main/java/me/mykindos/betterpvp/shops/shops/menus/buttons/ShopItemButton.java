package me.mykindos.betterpvp.shops.shops.menus.buttons;

import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShopItemButton extends Button {

    private final Menu parent;
    private final IShopItem shopItem;
    private final ItemStack item;

    public ShopItemButton(Menu parent, int slot, IShopItem shopItem, ItemStack item) {
        super(slot, item);
        this.parent = parent;
        this.shopItem = shopItem;
        this.item = item;
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;

        ShopCurrency currency = ShopCurrency.COINS;
        if (itemMeta.getPersistentDataContainer().has(ShopsNamespacedKeys.SHOP_CURRENCY)) {
            String currencyData = itemMeta.getPersistentDataContainer().get(ShopsNamespacedKeys.SHOP_CURRENCY, PersistentDataType.STRING);
            if (currencyData != null) {
                currency = ShopCurrency.valueOf(currencyData.toUpperCase());
            }
        }

        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            UtilServer.callEvent(new PlayerBuyItemEvent(player, gamer, shopItem, item, currency, clickType));
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            UtilServer.callEvent(new PlayerSellItemEvent(player, gamer, shopItem, item, currency, clickType));
        }

        parent.construct();
    }

    @Override
    public double getClickCooldown() {
        return 0.15;
    }

}
