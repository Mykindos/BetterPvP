package me.mykindos.betterpvp.shops.shops.menus.buttons;

import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.Inventory;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.SellAllMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SellAllSlot extends SlotElement.InventorySlotElement {

    private final SellAllMenu menu;

    public SellAllSlot(SellAllMenu menu, VirtualInventory inventory, int slot) {
        super(inventory, slot);
        this.menu = menu;
    }

    public SellAllSlot(SellAllMenu menu, VirtualInventory inventory, int slot, ItemProvider background) {
        super(inventory, slot, background);
        this.menu = menu;
    }

    @Override
    public boolean isSynced(String lang, ItemStack assumedStack) {
        return getInventory().isSynced(getSlot(), assumedStack) || Objects.equals(getItemStack(lang), assumedStack);
    }

    @Override
    public ItemStack getClickedItemStack(String lang, ItemStack assumedStack) {
        return getInventory().getItem(getSlot());
    }

    @Override
    public ItemStack getItemStack(String lang) {
        final int slot = getSlot();
        final ItemProvider background = getBackground();
        final Inventory inventory = getInventory();

        ItemStack itemStack = inventory.getUnsafeItem(slot);
        if (itemStack == null && background != null) {
            return background.get(lang);
        }

        if (itemStack == null) {
            return null;
        }

        ItemStack item = menu.getContext().getItemFactory().fromItemStack(itemStack).orElseThrow().getView().get();
        ItemView.ItemViewBuilder builder = ItemView.of(item).toBuilder();
        IShopItem shopItem = menu.getContext().getSellService().findMatchingShopItem(itemStack, menu.getContext().getShopItemsByKey());
        if (shopItem == null || shopItem.getSellPrice() <= 0) {
            builder.lore(Component.empty());
            builder.lore(Component.text("You cannot sell this!", NamedTextColor.RED));
            return builder.build().get();
        }

        int unitPrice = shopItem.getSellPrice();
        int stackPrice = unitPrice * itemStack.getAmount();
        ShopCurrency shopCurrency = menu.getContext().getCurrency(shopItem);

        // Add buy/sell
        builder.lore(Component.empty());
        builder.lore(Component.empty()
                .append(Component.text("Sell: ", NamedTextColor.GRAY))
                .append(menu.getContext().buildPriceComponent(shopCurrency, unitPrice).append(Component.text(" ea."))));

        builder.lore(Component.empty()
                .append(Component.text("This stack sells for ", NamedTextColor.GRAY))
                .append(menu.getContext().buildPriceComponent(shopCurrency, stackPrice)));

        return builder.build().get();
    }

}
