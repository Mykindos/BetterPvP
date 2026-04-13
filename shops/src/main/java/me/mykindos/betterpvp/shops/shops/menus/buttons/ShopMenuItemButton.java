package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.shops.shops.menus.ShopItemMenu;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class ShopMenuItemButton extends ControlItem<ShopMenu> {

    private static final int STACK_AMOUNT = 64;

    private final ShopMenu menu;
    private final IShopItem shopItem;

    @Override
    public ItemProvider getItemProvider(ShopMenu gui) {
        ShopCurrency currency = menu.getContext().getCurrency(shopItem);
        Component buyPrice = menu.getContext().buildPriceComponent(currency, shopItem.getBuyPrice() * STACK_AMOUNT);
        Component sellPrice = menu.getContext().buildPriceComponent(currency, shopItem.getSellPrice() * STACK_AMOUNT);

        return menu.getContext().createDisplayStack(shopItem, 1).toBuilder()
                .action(ClickActions.ALL, Component.empty()
                        .append(Component.text("Open")))
                .action(ClickActions.LEFT_SHIFT, Component.empty()
                        .append(Component.text("Buy " + STACK_AMOUNT + " for "))
                        .append(buyPrice))
                .action(ClickActions.RIGHT_SHIFT, Component.empty()
                        .append(Component.text("Sell " + STACK_AMOUNT + " for "))
                        .append(sellPrice))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType == ClickType.SHIFT_LEFT) {
            buyStack(player);
            menu.refresh();
            return;
        }

        if (clickType == ClickType.SHIFT_RIGHT) {
            sellStack(player);
            menu.refresh();
            return;
        }

        new ShopItemMenu(menu.getContext(), shopItem, menu).show(player);
    }

    private void buyStack(Player player) {
        Gamer gamer = menu.getContext().getClientManager().search().online(player).getGamer();
        ShopCurrency currency = menu.getContext().getCurrency(shopItem);
        ItemStack item = menu.getContext().getSellService().createShopItemStack(shopItem, STACK_AMOUNT);
        PlayerBuyItemEvent buyEvent = new PlayerBuyItemEvent(player, gamer, shopItem, item, currency);
        buyEvent.setRequestedAmount(STACK_AMOUNT);
        UtilServer.callEvent(buyEvent);

        if (!buyEvent.isCancelled()) {
            new SoundEffect("betterpvp", "shop.buy").play(player);
            new SoundEffect("betterpvp", "game.domination.gem_pickup", 2, 0.05f).play(player);
        }
    }

    private void sellStack(Player player) {
        Gamer gamer = menu.getContext().getClientManager().search().online(player).getGamer();
        ShopCurrency currency = menu.getContext().getCurrency(shopItem);
        ReferencingInventory inventory = ReferencingInventory.fromStorageContents(player.getInventory());
        PlayerSellItemEvent sellEvent = new PlayerSellItemEvent(player, gamer, shopItem, inventory, currency);
        sellEvent.setRequestedAmount(STACK_AMOUNT);
        UtilServer.callEvent(sellEvent);

        if (!sellEvent.isCancelled()) {
            new SoundEffect("betterpvp", "shop.sell").play(player);
            new SoundEffect("betterpvp", "game.domination.gem_pickup", 2, 0.05f).play(player);
        }
    }
}
