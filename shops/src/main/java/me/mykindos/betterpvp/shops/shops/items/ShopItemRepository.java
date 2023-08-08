package me.mykindos.betterpvp.shops.shops.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import java.util.HashMap;
import java.util.List;

@Singleton
public class ShopItemRepository {

    @Inject
    @Config(path = "shops.database.prefix", defaultValue = "shops_")
    private String databasePrefix;

    public HashMap<String, List<IShopItem>> getAllShopItems() {
        var shopItems = new HashMap<String, List<IShopItem>>();


        return shopItems;
    }

}
