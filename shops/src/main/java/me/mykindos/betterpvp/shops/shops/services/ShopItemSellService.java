package me.mykindos.betterpvp.shops.shops.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.shops.shops.items.DynamicShopItem;
import me.mykindos.betterpvp.shops.shops.items.ShopItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

@Singleton
@CustomLog
public class ShopItemSellService {

    private final ClientManager clientManager;
    private final ItemFactory itemFactory;

    @Inject
    public ShopItemSellService(ClientManager clientManager, ItemFactory itemFactory) {
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
    }

    /**
     * Result class for selling operations
     */
    public static class SellResult {
        public final boolean success;
        public final int amountSold;
        public final int totalEarned;
        public final String itemName;

        public SellResult(boolean success, int amountSold, int totalEarned, String itemName) {
            this.success = success;
            this.amountSold = amountSold;
            this.totalEarned = totalEarned;
            this.itemName = itemName;
        }
    }

    /**
     * Checks if an item can be sold to the given shop item
     */
    public boolean canSellItem(ItemStack item, IShopItem shopItem) {
        if (item == null || !(shopItem instanceof ShopItem castedShopItem)) {
            return false;
        }

        if (shopItem.getSellPrice() <= 0) {
            return false;
        }

        if (item.getType() != shopItem.getMaterial()) {
            return false;
        }

        // Check if item can't be sold
        if (item.getItemMeta().getPersistentDataContainer().has(CoreNamespaceKeys.SHOP_NOT_SELLABLE)) {
            return false;
        }

        // Check model data if needed
        ItemMeta itemMeta = item.getItemMeta();
        if (!castedShopItem.getItemFlags().containsKey("IGNORE_MODELDATA")) {
            if ((shopItem.getModelData() != 0 && !itemMeta.hasCustomModelData()) ||
                    (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() != shopItem.getModelData())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sells an item from player's inventory to the shop
     */
    public SellResult sellItem(Player player, ItemStack item, IShopItem shopItem, int amountToSell) {
        if (!canSellItem(item, shopItem)) {
            return new SellResult(false, 0, 0, "");
        }

        Gamer gamer = clientManager.search().online(player).getGamer();
        int cost = amountToSell * shopItem.getSellPrice();

        // Add currency to player (assuming coins for now, can be extended)
        gamer.saveProperty(GamerProperty.BALANCE.name(), gamer.getIntProperty(GamerProperty.BALANCE) + cost);

        // Update dynamic shop stock if applicable
        if (shopItem instanceof DynamicShopItem dynamicShopItem) {
            dynamicShopItem.setCurrentStock(Math.min(dynamicShopItem.getMaxStock(),
                    dynamicShopItem.getCurrentStock() + amountToSell));
        }

        // Log item UUID if available
        final ItemInstance instance = itemFactory.fromItemStack(item).orElseThrow();
        final Location location = player.getLocation();
        Optional<UUIDProperty> component = instance.getComponent(UUIDProperty.class);
        component.ifPresent(uuidProperty ->
                log.info("{} sold ({}) at {}", player.getName(), uuidProperty.getUniqueId(),
                                UtilWorld.locationToString((location)))
                        .setAction("ITEM_SELL")
                        .addClientContext(player)
                        .addItemContext(itemFactory.getItemRegistry(), instance)
                        .addLocationContext(location)
                        .submit());

        return new SellResult(true, amountToSell, cost, shopItem.getItemName());
    }

    /**
     * Removes items from player's inventory
     */
    public void removeItemFromInventory(Player player, int slot, int amount) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null) return;

        if (item.getAmount() <= amount) {
            player.getInventory().setItem(slot, new ItemStack(Material.AIR));
        } else {
            ItemStack newStack = item.clone();
            newStack.setAmount(item.getAmount() - amount);
            player.getInventory().setItem(slot, newStack);
        }
    }

    /**
     * Finds matching shop item for a given item stack
     */
    public IShopItem findMatchingShopItem(ItemStack item, List<IShopItem> shopItems) {
        for (IShopItem shopItem : shopItems) {
            if (canSellItem(item, shopItem)) {
                return shopItem;
            }
        }
        return null;
    }
}