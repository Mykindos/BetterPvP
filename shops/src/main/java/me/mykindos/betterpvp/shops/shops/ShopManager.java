package me.mykindos.betterpvp.shops.shops;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.shops.shops.items.DynamicShopItem;
import me.mykindos.betterpvp.shops.shops.items.ShopItemRepository;
import me.mykindos.betterpvp.shops.shops.menus.SellAllMenu;
import me.mykindos.betterpvp.shops.shops.menus.ShopContext;
import me.mykindos.betterpvp.shops.shops.menus.ShopItemMenu;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import me.mykindos.betterpvp.shops.shops.services.ShopItemSellService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Getter
@CustomLog
public class ShopManager {

    private final ShopItemRepository shopItemRepository;
    private final ShopItemSellService shopItemSellService;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;

    private Map<String, List<IShopItem>> shopItems = new HashMap<>();

    @Inject
    public ShopManager(ShopItemRepository shopItemRepository, ShopItemSellService shopItemSellService,
                       ItemFactory itemFactory, ClientManager clientManager) {
        this.shopItemRepository = shopItemRepository;
        this.shopItemSellService = shopItemSellService;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        loadShopItems();
    }

    public void loadShopItems() {
        shopItemRepository.copyTemplatedDynamicPrices();
        shopItems = shopItemRepository.getAllShopItems();
    }

    public List<IShopItem> getShopItems(String shopkeeper) {
        return shopItems.get(shopkeeper);
    }

    /**
     * Shows the specified Shop GUI to the player.
     */
    public void showShopMenu(Player player, String shopkeeper) {
        List<IShopItem> shopkeeperItems = getShopItems(shopkeeper);
        if (shopkeeperItems == null || shopkeeperItems.isEmpty()) return;

        List<IShopItem> allItems = shopItems.values().stream().flatMap(Collection::stream).toList();
        ShopContext context = new ShopContext(itemFactory, clientManager, shopItemSellService, allItems);
        new ShopMenu(shopkeeper, shopkeeperItems, context).show(player);
        log.info("{} opened Shop: {}", player.getName(), shopkeeper).submit();
    }

    public void notifyDynamicPriceChanged(DynamicShopItem shopItem) {
        for (Window window : WindowManager.getInstance().getWindows()) {
            if (!(window instanceof AbstractSingleWindow singleWindow)) {
                continue;
            }

            if (singleWindow.getGui() instanceof ShopMenu menu && menu.containsShopItem(shopItem)) {
                menu.refresh();
            } else if (singleWindow.getGui() instanceof ShopItemMenu menu && menu.isViewingShopItem(shopItem)) {
                menu.notifyOpenWindows();
            } else if (singleWindow.getGui() instanceof SellAllMenu menu && menu.containsShopItem(shopItem)) {
                menu.notifyPriceChanged();
            }
        }
    }
}
