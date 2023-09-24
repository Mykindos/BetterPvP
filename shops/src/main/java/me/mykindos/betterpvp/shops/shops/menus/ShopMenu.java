package me.mykindos.betterpvp.shops.shops.menus;

import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.shops.shops.menus.buttons.ShopItemButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ShopMenu extends Menu {

    private final List<IShopItem> shopItems;
    private final ItemHandler itemHandler;

    public ShopMenu(Player player, Component title, List<IShopItem> shopItems, ItemHandler itemHandler) {
        super(player, 54, title);
        this.shopItems = shopItems;
        this.itemHandler = itemHandler;
        loadShop();
    }

    private void loadShop() {
        for (IShopItem shopItem : shopItems) {
            var itemStack = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());

            addButton(new ShopItemButton(shopItem.getSlot(), shopItem, addShopLore(itemHandler.updateNames(itemStack), shopItem)));
        }
    }

    private ItemStack addShopLore(ItemStack item, IShopItem shopItem) {
        var itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            var lore = itemMeta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }

            boolean canSell = shopItem.getSellPrice() > 0;

            lore.add(Component.empty());
            lore.add(Component.text("Buy Price: ", NamedTextColor.GRAY).append(Component.text(NumberFormat.getInstance().format(shopItem.getBuyPrice()), NamedTextColor.YELLOW)));

            int indexToInsertSellPrice = lore.size(); // Get the index to insert the Sell Price

            lore.add(Component.empty());
            lore.add(Component.text("Shift Left Click: ", NamedTextColor.GRAY).append(Component.text("Buy 64", NamedTextColor.YELLOW)));

            if (canSell) {
                lore.add(indexToInsertSellPrice, Component.text("Sell Price: ", NamedTextColor.GRAY)
                        .append(Component.text(NumberFormat.getInstance().format(shopItem.getSellPrice()), NamedTextColor.YELLOW)));
                lore.add(lore.size(), Component.text("Shift Right Click: ", NamedTextColor.GRAY)
                        .append(Component.text("Sell 64", NamedTextColor.YELLOW)));
            }

            itemMeta.lore(lore);
            item.setItemMeta(itemMeta);


        }
        return item;
    }


}
