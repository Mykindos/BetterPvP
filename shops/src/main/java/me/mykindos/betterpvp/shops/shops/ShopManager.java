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
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.menu.impl.PagedSingleWindow;
import me.mykindos.betterpvp.shops.shops.items.ShopItemRepository;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

@Singleton
@Getter
@CustomLog
public class ShopManager {

    private final ShopItemRepository shopItemRepository;

    private HashMap<String, List<IShopItem>> shopItems = new HashMap<>();

    @Inject
    public ShopManager(ShopItemRepository shopItemRepository) {
        this.shopItemRepository = shopItemRepository;
        loadShopItems();
    }

    public void loadShopItems() {
        shopItems = shopItemRepository.getAllShopItems();
    }

    public List<IShopItem> getShopItems(String shopkeeper) {
        return shopItems.get(shopkeeper);
    }

    /**
     * Shows the specified Shop GUI to the player
     * @param player the player to show
     * @param shopkeeper the name of the shopkeeper
     * @param itemHandler the itemHandler, to pass to ShopMenu
     * @param clientManager the clientManager, to pass to ShopMenu
     */
    public void showShopMenu(Player player, String shopkeeper, ItemHandler itemHandler, ClientManager clientManager) {
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
                        "x x x < - > x x x")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(null))
                .addIngredient('>', new ForwardButton());
        for (int i = 1; i <= maxPages; i++) {
            builder.addContent(new ShopMenu(i,
                    shopkeeperItems,
                    itemHandler,
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
