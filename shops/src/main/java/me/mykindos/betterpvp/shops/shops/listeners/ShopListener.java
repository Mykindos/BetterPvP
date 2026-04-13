package me.mykindos.betterpvp.shops.shops.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.FinalPlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.FinalPlayerSellItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.inventory.inventory.Inventory;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import me.mykindos.betterpvp.shops.shops.items.DynamicShopItem;
import me.mykindos.betterpvp.shops.shops.items.ShopItem;
import me.mykindos.betterpvp.shops.shops.services.ShopItemSellService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
@CustomLog
public class ShopListener implements Listener {

    private final ItemRegistry registry;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final ShopItemSellService shopItemSellService;
    private final ShopManager shopManager;

    @Inject
    public ShopListener(ItemRegistry registry, ItemFactory itemFactory, ClientManager clientManager,
                        ShopItemSellService shopItemSellService, ShopManager shopManager) {
        this.registry = registry;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        this.shopItemSellService = shopItemSellService;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBuyItem(PlayerBuyItemEvent event) {
        int requestedAmount = event.getRequestedAmount();
        int cost = event.getShopItem().getBuyPrice() * requestedAmount;

        if (event.getCurrency() == ShopCurrency.COINS) {
            if (event.getGamer().getIntProperty(GamerProperty.BALANCE) < cost) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        } else if (event.getCurrency() == ShopCurrency.BARK) {
            if (!UtilInventory.contains(event.getPlayer(), "progression:tree_bark", cost)) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        }

        if (event.getPlayer().getInventory().firstEmpty() == -1) {
            event.cancel("Your inventory is full.");
            return;
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalBuyItem(PlayerBuyItemEvent event) {
        if (event.isCancelled()) {
            UtilMessage.message(event.getPlayer(), "Shop", event.getCancelReason());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            return;
        }

        final IShopItem shopItem = event.getShopItem();
        int amount = event.getRequestedAmount();
        int cost = amount * shopItem.getBuyPrice();
        if (event.getCurrency() == ShopCurrency.COINS) {
            event.getGamer().saveProperty(GamerProperty.BALANCE.name(), event.getGamer().getIntProperty(GamerProperty.BALANCE) - cost);
        } else if (event.getCurrency() == ShopCurrency.BARK) {
            Player player = event.getPlayer();
            UtilInventory.remove(player, "progression:tree_bark", cost);
        }

        if (event.getShopItem() instanceof DynamicShopItem dynamicShopItem) {
            setCurrentStockAndNotify(dynamicShopItem, Math.max(0, dynamicShopItem.getCurrentStock() - amount));
        }

        ItemStack boughtItem = shopItemSellService.createShopItemStack(event.getShopItem(), amount);
        final ItemInstance instanceResult = itemFactory.fromItemStack(boughtItem).orElseThrow();
        final ItemStack result = instanceResult.createItemStack();
        UtilItem.insert(event.getPlayer(), result);
        UtilMessage.simpleMessage(event.getPlayer(), "Shop", "You have purchased <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                amount, shopItemSellService.getItemName(event.getShopItem()), NumberFormat.getInstance().format(cost), event.getCurrency().name().toLowerCase());
        log.info("{} purchased {}x {} for {} {}",
                        event.getPlayer().getName(), amount, shopItemSellService.getItemName(event.getShopItem()), cost, event.getCurrency().name().toLowerCase())
                .setAction("SHOP_BUY").addClientContext(event.getPlayer())
                .addContext("ShopItem", shopItemSellService.getItemName(event.getShopItem())).addContext("Amount", amount + "")
                .addContext("Price", cost + "").submit();

        // Log the item purchase with UUID
        instanceResult.getComponent(UUIDProperty.class).ifPresent(uuidProperty -> {
            final UUID uuid = uuidProperty.getUniqueId();
            Player player = event.getPlayer();
            Location location = player.getLocation();
            log.info("{} purchased ({}) at {}", player.getName(), uuid,
                            UtilWorld.locationToString((location))).setAction("ITEM_BUY")
                    .addClientContext(player)
                    .addItemContext(registry, instanceResult)
                    .addLocationContext(location)
                    .submit();
        });

        new FinalPlayerBuyItemEvent(event.getPlayer(), event.getGamer(), event.getShopItem(), event.getItem(), event.getCurrency(), amount, cost).callEvent();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSellItem(PlayerSellItemEvent event) {
        if (event.getShopItem().getSellPrice() == 0) {
            event.cancel("You cannot sell this item.");
            return;
        }

        int count = 0;
        for (ItemStack content : event.getInventory().getItems()) {
            if (content == null) continue;
            if (shopItemSellService.canSellItem(content, event.getShopItem())) {
                count += content.getAmount();
            }
        }
        if (count < event.getRequestedAmount()) {
            event.cancel("You do not have enough of this item to sell.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalSellItem(PlayerSellItemEvent event) {
        Player player = event.getPlayer();
        ShopItem shopItem = (ShopItem) event.getShopItem();

        if (event.isCancelled()) {
            UtilMessage.message(player, "Shop", event.getCancelReason());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            // For staged sells the items live in a VirtualInventory; return them so they are
            // not silently discarded when the menu closes.
            if (event.getInventory() instanceof VirtualInventory virtualInventory) {
                for (int i = 0; i < virtualInventory.getSize(); i++) {
                    ItemStack item = virtualInventory.getItem(i);
                    if (item != null && shopItemSellService.canSellItem(item, shopItem)) {
                        UtilItem.insert(player, item);
                        virtualInventory.setItem(null, i, null);
                    }
                }
            }
            return;
        }

        // Remove matching items from whichever inventory was provided (VirtualInventory or
        // ReferencingInventory), logging UUIDs as we go and capturing a representative stack
        // for the FinalPlayerSellItemEvent.
        Inventory inv = event.getInventory();
        int remaining = event.getRequestedAmount();
        ItemStack representative = null;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !shopItemSellService.canSellItem(item, shopItem)) continue;
            if (representative == null) representative = item.clone();
            shopItemSellService.logSellUUID(player, item);
            int take = Math.min(item.getAmount(), remaining);
            remaining -= take;
            if (take >= item.getAmount()) {
                inv.setItem(null, i, null);
            } else {
                ItemStack reduced = item.clone();
                reduced.setAmount(item.getAmount() - take);
                inv.setItem(null, i, reduced);
            }
        }

        if (remaining > 0) {
            // Items were present at validation time but are gone now; abort without side effects.
            UtilMessage.message(player, "Shop", "You do not have enough of this item to sell.");
            return;
        }

        int totalSold = event.getRequestedAmount();
        int totalEarned = totalSold * shopItem.getSellPrice();
        String itemName = shopItemSellService.getItemName(shopItem);

        event.getGamer().saveProperty(GamerProperty.BALANCE.name(),
                event.getGamer().getIntProperty(GamerProperty.BALANCE) + totalEarned);

        if (shopItem instanceof DynamicShopItem dynamicShopItem) {
            setCurrentStockAndNotify(dynamicShopItem, Math.min(dynamicShopItem.getMaxStock(),
                    dynamicShopItem.getCurrentStock() + totalSold));
        }

        UtilMessage.simpleMessage(player, "Shop", "You have sold <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                totalSold, itemName, UtilFormat.formatNumber(totalEarned), event.getCurrency().name().toLowerCase());

        log.info("{} sold {}x {} for {} {}", player.getName(), totalSold, itemName, totalEarned, event.getCurrency().name().toLowerCase())
                .setAction("SHOP_SELL").addClientContext(player)
                .addContext("ShopItem", itemName).addContext("Amount", totalSold + "")
                .addContext("Price", totalEarned + "").submit();

        new FinalPlayerSellItemEvent(player, event.getGamer(), event.getShopItem(), representative, event.getCurrency(), totalSold, totalEarned).callEvent();
    }

    @UpdateEvent(delay = 180_000) // 3 minutes
    public void updateDynamicPrices() {
        List<DynamicShopItem> dynamicShopItems = new ArrayList<>();
        shopManager.getShopItems().values().forEach(shopItems -> {
            shopItems.forEach(shopItem -> {
                if (shopItem instanceof DynamicShopItem dynamicShopItem) {
                    dynamicShopItems.add(dynamicShopItem);
                }
            });
        });

        shopManager.getShopItemRepository().updateStock(dynamicShopItems);
    }

    @UpdateEvent(delay = 3_600_000) // 2 Hours
    public void refreshStocks() {
        shopManager.getShopItems().values().forEach(shopItems -> {
            shopItems.forEach(shopItem -> {
                if (shopItem instanceof DynamicShopItem dynamicShopItem) {
                    if (dynamicShopItem.getCurrentStock() < dynamicShopItem.getBaseStock()) {
                        setCurrentStockAndNotify(dynamicShopItem, (int) (dynamicShopItem.getCurrentStock() + (dynamicShopItem.getBaseStock() / 15)));
                    } else if (dynamicShopItem.getCurrentStock() > dynamicShopItem.getBaseStock()) {
                        setCurrentStockAndNotify(dynamicShopItem, (int) (dynamicShopItem.getCurrentStock() - (dynamicShopItem.getBaseStock() / 15)));
                    }
                }
            });
        });

        UtilMessage.simpleBroadcast("Shop", "Dynamic prices have been updated!",
                Component.text("This means that buy / sell prices on farming items have been adjusted to reflect the current market.", NamedTextColor.GRAY));
    }

    private void setCurrentStockAndNotify(DynamicShopItem shopItem, int currentStock) {
        int oldBuyPrice = shopItem.getBuyPrice();
        int oldSellPrice = shopItem.getSellPrice();
        shopItem.setCurrentStock(currentStock);
        if (oldBuyPrice != shopItem.getBuyPrice() || oldSellPrice != shopItem.getSellPrice()) {
            shopManager.notifyDynamicPriceChanged(shopItem);
        }
    }

}
