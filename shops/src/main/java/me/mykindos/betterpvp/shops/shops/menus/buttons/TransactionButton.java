package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.ShopItemMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class TransactionButton extends ControlItem<ShopItemMenu> {

    private final ShopItemMenu itemMenu;
    private final boolean buy;

    @Override
    public ItemProvider getItemProvider(ShopItemMenu gui) {
        final int amount = itemMenu.getAmount();
        final ShopCurrency currency = itemMenu.getContext().getCurrency(itemMenu.getShopItem());
        final int unitPrice = buy ? itemMenu.getShopItem().getBuyPrice() : itemMenu.getShopItem().getSellPrice();
        final int total = amount * unitPrice;

        final Component component = itemMenu.getContext().buildPriceComponent(currency, total);

        if (!buy && itemMenu.getShopItem().getSellPrice() <= 0) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .displayName(Component.empty()
                            .append(Component.text("You cannot sell this!", NamedTextColor.RED)))
                    .build();
        }

        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Component.empty()
                        .append(Component.text(buy ? "Buy" : "Sell", NamedTextColor.GREEN))
                        .appendSpace()
                        .append(Component.text(amount, NamedTextColor.WHITE))
                        .appendSpace()
                        .append(Component.text("for", NamedTextColor.GRAY))
                        .appendSpace()
                        .append(component))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Gamer gamer = itemMenu.getContext().getClientManager().search().online(player).getGamer();
        ShopCurrency currency = itemMenu.getContext().getCurrency(itemMenu.getShopItem());
        if (buy) {
            ItemStack item = itemMenu.getContext().getSellService().createShopItemStack(itemMenu.getShopItem(), itemMenu.getAmount());
            PlayerBuyItemEvent buyEvent = new PlayerBuyItemEvent(player, gamer, itemMenu.getShopItem(), item, currency);
            buyEvent.setRequestedAmount(itemMenu.getAmount());
            UtilServer.callEvent(buyEvent);

            if (!buyEvent.isCancelled()) {
                new SoundEffect("betterpvp", "shop.buy").play(player);
                new SoundEffect("betterpvp", "game.domination.gem_pickup", 2, 0.05f).play(player);
            }
        } else {
            final ReferencingInventory inventory = ReferencingInventory.fromStorageContents(player.getInventory());
            PlayerSellItemEvent sellEvent = new PlayerSellItemEvent(player, gamer, itemMenu.getShopItem(), inventory, currency);
            sellEvent.setRequestedAmount(itemMenu.getAmount());
            UtilServer.callEvent(sellEvent);

            if (!sellEvent.isCancelled()) {
                new SoundEffect("betterpvp", "shop.sell").play(player);
                new SoundEffect("betterpvp", "game.domination.gem_pickup", 2, 0.05f).play(player);
            }
        }
        itemMenu.notifyOpenWindows();
    }
}
