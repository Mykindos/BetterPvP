package me.mykindos.betterpvp.shops.shops;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.menu.impl.PagedSingleWindow;
import me.mykindos.betterpvp.shops.shops.items.ShopItemRepository;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SellAllButton;
import me.mykindos.betterpvp.shops.shops.services.ShopItemSellService;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Getter
@CustomLog
public class ShopManager {

    private final ShopItemRepository shopItemRepository;
    private final ShopItemSellService shopItemSellService;

    private Map<String, List<IShopItem>> shopItems = new HashMap<>();

    @Inject
    public ShopManager(ShopItemRepository shopItemRepository, ShopItemSellService shopItemSellService) {
        this.shopItemRepository = shopItemRepository;
        this.shopItemSellService = shopItemSellService;
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
     * Shows the specified Shop GUI to the player
     * @param player the player to show
     * @param shopkeeper the name of the shopkeeper
     * @param itemFactory the itemFactory, to pass to ShopMenu
     * @param clientManager the clientManager, to pass to ShopMenu
     */
    public void showShopMenu(Player player, String shopkeeper, ItemFactory itemFactory, ClientManager clientManager) {
        List<IShopItem> shopkeeperItems = getShopItems(shopkeeper);
        if (shopkeeperItems == null || shopkeeperItems.isEmpty()) return;

        int maxPages = getShopItems(shopkeeper).stream()
                .map(IShopItem::getPage)
                .max(Integer::compareTo)
                .orElse(0);
        // 50 is the max slot
        PagedGui.Builder<Gui> builder = PagedGui.guis();
        builder.setStructure("x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x < - > x x s")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(null))
                .addIngredient('>', new ForwardButton())
                .addIngredient('s', new SellAllButton(shopkeeperItems, clientManager, itemHandler, shopItemSellService));
        for (int i = 1; i <= maxPages; i++) {
            builder.addContent(new ShopMenu(i,
                    shopkeeperItems,
                    itemFactory,
                    clientManager)
            );
        }
        PagedSingleWindow window = (PagedSingleWindow) PagedSingleWindow.builder()
                .setTitle(shopkeeper + " (1)")
                .setGui(builder)
                .build(player);
        window.addPageChangeHandler((current, next) -> window.changeTitle(shopkeeper + " (" + (next + 1) + ")"));
        window.open();
        log.info("{} opened Shop: {}", player.getName(), shopkeeper).submit();
    }
}
