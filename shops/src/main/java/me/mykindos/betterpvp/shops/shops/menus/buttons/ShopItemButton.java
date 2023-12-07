package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.NonNull;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.NumberFormat;

public class ShopItemButton extends AbstractItem implements CooldownButton {

    private final IShopItem shopItem;
    private final ClientManager clientManager;
    private final ItemHandler itemHandler;

    public ShopItemButton(@NonNull IShopItem shopItem, ItemHandler handler, @NonNull ClientManager clientManager) {
        this.shopItem = shopItem;
        this.clientManager = clientManager;
        this.itemHandler = handler;
    }

    @Override
    public ItemProvider getItemProvider() {
        boolean canSell = shopItem.getSellPrice() > 0;
        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(shopItem.getMaterial())
                .amount(shopItem.getAmount())
                .lore(Component.empty())
                .lore(Component.text("Buy: ", NamedTextColor.GRAY).append(Component.text("$" + NumberFormat.getInstance().format(shopItem.getBuyPrice()), NamedTextColor.YELLOW)))
                .lore(Component.text("Shift Left Click: ", NamedTextColor.GRAY).append(Component.text("Buy 64", NamedTextColor.YELLOW)));

        if (canSell) {
            builder.lore(Component.empty());
            builder.lore(Component.text("Sell: ", NamedTextColor.GRAY).append(Component.text("$" + NumberFormat.getInstance().format(shopItem.getSellPrice()), NamedTextColor.YELLOW)));
            builder.lore(Component.text("Shift Right Click: ", NamedTextColor.GRAY).append(Component.text("Sell 64", NamedTextColor.YELLOW)));
        }

        final ItemStack item = itemHandler.updateNames(builder.build().get());
        return ItemView.of(item);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final ItemStack item = getItemProvider().get();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        ShopCurrency currency = ShopCurrency.COINS;
        final PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        if (pdc.has(ShopsNamespacedKeys.SHOP_CURRENCY)) {
            String currencyData = pdc.get(ShopsNamespacedKeys.SHOP_CURRENCY, PersistentDataType.STRING);
            if (currencyData != null) {
                currency = ShopCurrency.valueOf(currencyData.toUpperCase());
            }
        }

        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            UtilServer.callEvent(new PlayerBuyItemEvent(player, gamer, shopItem, item, currency, clickType));
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            UtilServer.callEvent(new PlayerSellItemEvent(player, gamer, shopItem, item, currency, clickType));
        }

        notifyWindows();
    }

    @Override
    public double getCooldown() {
        return 0.2;
    }
}
