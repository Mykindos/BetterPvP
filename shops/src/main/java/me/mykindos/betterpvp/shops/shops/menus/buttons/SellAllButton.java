package me.mykindos.betterpvp.shops.shops.menus.buttons;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.services.ShopItemSellService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
@CustomLog
public class SellAllButton extends AbstractItem implements CooldownButton {

    private final List<IShopItem> shopItems;
    private final ShopItemSellService shopItemService;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder().material(Material.BARRIER)
                .customModelData(2)
                .displayName(Component.text("Sell All Items", NamedTextColor.YELLOW))
                .amount(1)
                .lore(Component.text("Click to sell all items in your inventory", NamedTextColor.GRAY))
                .lore(Component.text("that can be sold to this Shopkeeper", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Window openWindow = WindowManager.getInstance().getOpenWindow(player);
        new ConfirmationMenu("Are you sure you want to sell all items?", success -> {
            if (Boolean.TRUE.equals(success)) {
                SellSummary summary = processInventorySales(player);
                provideFeedback(player, summary);
            }

            if (openWindow != null) {
                openWindow.open();
            }
        }).show(player);
    }

    private SellSummary processInventorySales(Player player) {
        int totalSold = 0;
        int totalEarned = 0;
        StringBuilder soldItemsLog = new StringBuilder();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;

            IShopItem matchingShopItem = shopItemService.findMatchingShopItem(item, shopItems);
            if (matchingShopItem == null) continue;

            int amount = item.getAmount();
            ShopItemSellService.SellResult result = shopItemService.sellItem(player, item, matchingShopItem, amount);
            if (!result.success) continue;

            shopItemService.removeItemFromInventory(player, i, amount);
            totalSold += result.amountSold;
            totalEarned += result.totalEarned;

            if (soldItemsLog.length() > 0) {
                soldItemsLog.append(", ");
            }
            soldItemsLog.append(result.amountSold).append("x ").append(result.itemName);
        }

        return new SellSummary(totalSold, totalEarned, soldItemsLog.toString());
    }

    private void provideFeedback(Player player, SellSummary summary) {
        if (summary.totalSold > 0) {
            UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f, false);
            UtilMessage.simpleMessage(player, "Shop", "You have sold <alt2>%d items</alt2> for <alt2>%s coins</alt2>.",
                    summary.totalSold, UtilFormat.formatNumber(summary.totalEarned));

            log.info("{} sold {} items for {} coins: {}",
                            player.getName(), summary.totalSold, summary.totalEarned, summary.log)
                    .setAction("SHOP_SELL_ALL")
                    .addClientContext(player)
                    .addContext("TotalItems", summary.totalSold + "")
                    .addContext("TotalPrice", summary.totalEarned + "")
                    .submit();
        } else {
            UtilMessage.message(player, "Shop", "You don't have any items that can be sold to this shopkeeper.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
        }
    }

    private record SellSummary(int totalSold, int totalEarned, String log) {}

    @Override
    public double getCooldown() {
        return 1.0;
    }
}