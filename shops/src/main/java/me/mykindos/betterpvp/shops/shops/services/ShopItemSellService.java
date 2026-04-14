package me.mykindos.betterpvp.shops.shops.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@CustomLog
public class ShopItemSellService {

    private final ItemFactory itemFactory;

    @Inject
    public ShopItemSellService(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Checks if an item can be sold to the given shop item
     */
    public boolean canSellItem(ItemStack item, IShopItem shopItem) {
        if (shopItem.getSellPrice() <= 0) {
            return false;
        }

        String itemKey = getSellableItemKey(item);
        return Objects.equals(itemKey, shopItem.getItemKey());
    }

    /**
     * Finds matching shop item for a given item stack
     */
    public IShopItem findMatchingShopItem(ItemStack item, List<IShopItem> shopItems) {
        return findMatchingShopItem(item, createShopItemIndex(shopItems));
    }

    public IShopItem findMatchingShopItem(ItemStack item, Map<String, IShopItem> shopItemsByKey) {
        String itemKey = getSellableItemKey(item);
        if (itemKey == null) {
            return null;
        }

        IShopItem shopItem = shopItemsByKey.get(itemKey);
        return shopItem != null && shopItem.getSellPrice() > 0 ? shopItem : null;
    }

    public Map<String, IShopItem> createShopItemIndex(Collection<IShopItem> shopItems) {
        Map<String, IShopItem> shopItemsByKey = new LinkedHashMap<>();
        for (IShopItem shopItem : shopItems) {
            shopItemsByKey.putIfAbsent(shopItem.getItemKey(), shopItem);
        }
        return Map.copyOf(shopItemsByKey);
    }

    private String getSellableItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.getPersistentDataContainer().has(CoreNamespaceKeys.SHOP_NOT_SELLABLE)) {
                return null;
            }

            String customItemKey = meta.getPersistentDataContainer().get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
            NamespacedKey key = customItemKey == null ? null : NamespacedKey.fromString(customItemKey);
            if (key != null && itemFactory.getItemRegistry().getItem(key) != null) {
                return key.asString();
            }
        }

        return itemFactory.fromItemStack(item)
                .map(instance -> itemFactory.getItemRegistry().getKey(instance.getBaseItem()))
                .map(NamespacedKey::asString)
                .orElse(null);
    }

    /**
     * Logs the UUID of a sold item instance, if it has one.
     * Called once per inventory slot involved in a sell transaction.
     */
    public void logSellUUID(Player player, ItemStack item) {
        itemFactory.fromItemStack(item).ifPresent(instance -> {
            final Location location = player.getLocation();
            instance.getComponent(UUIDProperty.class).ifPresent(uuidProperty ->
                    log.info("{} sold ({}) at {}", player.getName(), uuidProperty.getUniqueId(),
                                    UtilWorld.locationToString(location))
                            .setAction("ITEM_SELL")
                            .addClientContext(player)
                            .addItemContext(itemFactory.getItemRegistry(), instance)
                            .addLocationContext(location)
                            .submit());
        });
    }

    public ItemStack createShopItemStack(IShopItem shopItem, int amount) {
        ItemStack result = itemFactory.create(itemFactory.getItemRegistry().getItem(shopItem.getItemKey())).createItemStack();
        result.setAmount(amount);
        return result;
    }

    public String getItemName(IShopItem shopItem) {
        return PlainTextComponentSerializer.plainText()
                .serialize(itemFactory.create(itemFactory.getItemRegistry().getItem(shopItem.getItemKey())).getView().getName());
    }
}
