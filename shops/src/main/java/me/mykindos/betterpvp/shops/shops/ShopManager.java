package me.mykindos.betterpvp.shops.shops;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.shops.shops.items.IShopItem;
import me.mykindos.betterpvp.shops.shops.items.ShopItemRepository;

import java.util.HashMap;
import java.util.List;

@Singleton
@Getter
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
}
