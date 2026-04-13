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
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
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
        boolean isShifting = event.getClickType().name().contains("SHIFT");

        //Optional<IWeapon> weaponByItemStack = weaponManager.getWeaponByItemStack(event.getItem());
        //if(isShifting && ((weaponByItemStack.isPresent() && weaponByItemStack.get() instanceof LegendaryWeapon)
        //        || event.getItem().getItemMeta() instanceof Damageable)) {
        //    isShifting = false;
        //}

        if (event.getItem().getMaxStackSize() == 1) {
            isShifting = false;
        }

        int cost = isShifting ? event.getShopItem().getBuyPrice() * 64 : event.getShopItem().getBuyPrice();

        if (event.getCurrency() == ShopCurrency.COINS) {
            if (event.getGamer().getIntProperty(GamerProperty.BALANCE) < cost) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        } else if (event.getCurrency() == ShopCurrency.FRAGMENTS) {
            if (event.getGamer().getIntProperty(GamerProperty.FRAGMENTS) < cost) {
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

        boolean isShifting = event.getClickType().name().contains("SHIFT");

        //Optional<IWeapon> weaponByItemStack = weaponManager.getWeaponByItemStack(event.getItem());
        //if(isShifting && (weaponByItemStack.isPresent() && weaponByItemStack.get() instanceof LegendaryWeapon)) {
        //    isShifting = false;
        //}

        if (event.getItem().getMaxStackSize() == 1) {
            isShifting = false;
        }

        final IShopItem shopItem = event.getShopItem();

        int amount;
        int cost;
        if (shopItem.getAmount() == 1) {
            amount = isShifting ? 64 : shopItem.getAmount();
            cost = amount * shopItem.getBuyPrice();
        } else {
            amount = shopItem.getAmount();
            cost = shopItem.getBuyPrice();
        }


        if (event.getCurrency() == ShopCurrency.COINS) {
            event.getGamer().saveProperty(GamerProperty.BALANCE.name(), event.getGamer().getIntProperty(GamerProperty.BALANCE) - cost);
        } else if (event.getCurrency() == ShopCurrency.FRAGMENTS) {
            event.getGamer().saveProperty(GamerProperty.FRAGMENTS.name(), event.getGamer().getIntProperty(GamerProperty.FRAGMENTS) - cost);
        } else if (event.getCurrency() == ShopCurrency.BARK) {
            Player player = event.getPlayer();
            UtilInventory.remove(player, "progression:tree_bark", cost);
        }

        if (event.getShopItem() instanceof DynamicShopItem dynamicShopItem) {
            dynamicShopItem.setCurrentStock(Math.max(0, dynamicShopItem.getCurrentStock() - amount));
        }

        ItemStack boughtItem = new ItemStack(event.getShopItem().getMaterial(), amount);
        boughtItem.editMeta(meta -> {
            if (event.getShopItem().getModelData() != 0) {
                meta.setCustomModelData(event.getShopItem().getModelData());
            }
        });

        final ItemInstance instanceResult = itemFactory.fromItemStack(boughtItem).orElseThrow();
        final ItemStack result = instanceResult.createItemStack();
        UtilItem.insert(event.getPlayer(), result);
        UtilMessage.simpleMessage(event.getPlayer(), "Shop", "You have purchased <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                amount, event.getShopItem().getItemName(), NumberFormat.getInstance().format(cost), event.getCurrency().name().toLowerCase());
        UtilSound.playSound(event.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f, false);
        log.info("{} purchased {}x {} for {} {}",
                        event.getPlayer().getName(), amount, event.getShopItem().getItemName(), cost, event.getCurrency().name().toLowerCase())
                .setAction("SHOP_BUY").addClientContext(event.getPlayer())
                .addContext("ShopItem", event.getShopItem().getItemName()).addContext("Amount", amount + "")
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

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalSellItem(PlayerSellItemEvent event) {
        Player player = event.getPlayer();

        if (event.isCancelled()) {
            UtilMessage.message(event.getPlayer(), "Shop", event.getCancelReason());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            return;
        }

        ShopItem shopItem = (ShopItem) event.getShopItem();
        boolean isShifting = event.getClickType().name().contains("SHIFT");

        if (player.getInventory().contains(event.getItem().getType())) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;

                if (shopItemSellService.canSellItem(item, shopItem)) {
                    int amount = shopItem.getAmount() == 1
                        ? (isShifting ? item.getAmount() : shopItem.getAmount())
                        : shopItem.getAmount();

                    if (item.getAmount() >= amount) {
                        ShopItemSellService.SellResult result = shopItemSellService.sellItem(player, item, shopItem, amount);
                        if (result.success) {
                            shopItemSellService.removeItemFromInventory(player, i, amount);

                            UtilSound.playSound(event.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f, false);
                            UtilMessage.simpleMessage(event.getPlayer(), "Shop", "You have sold <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                                    result.amountSold, result.itemName, UtilFormat.formatNumber(result.totalEarned), event.getCurrency().name().toLowerCase());

                            log.info("{} sold {}x {} for {} {}",
                                    event.getPlayer().getName(), result.amountSold, result.itemName, result.totalEarned, event.getCurrency().name().toLowerCase())
                                    .setAction("SHOP_SELL").addClientContext(event.getPlayer())
                                    .addContext("ShopItem", result.itemName).addContext("Amount", result.amountSold + "")
                                    .addContext("Price", result.totalEarned + "").submit();

                            new FinalPlayerSellItemEvent(player, event.getGamer(), event.getShopItem(), event.getItem(), event.getCurrency(), result.amountSold, result.totalEarned).callEvent();
                        }
                        return;
                    }
                }
            }
        }
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
                        dynamicShopItem.setCurrentStock((int) (dynamicShopItem.getCurrentStock() + (dynamicShopItem.getBaseStock() / 15)));
                    } else if (dynamicShopItem.getCurrentStock() > dynamicShopItem.getBaseStock()) {
                        dynamicShopItem.setCurrentStock((int) (dynamicShopItem.getCurrentStock() - (dynamicShopItem.getBaseStock() / 15)));
                    }
                }
            });
        });

        UtilMessage.simpleBroadcast("Shop", "Dynamic prices have been updated!",
                Component.text("This means that buy / sell prices on farming items have been adjusted to reflect the current market.", NamedTextColor.GRAY));
    }

}