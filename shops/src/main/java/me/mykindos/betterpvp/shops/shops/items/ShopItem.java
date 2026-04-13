package me.mykindos.betterpvp.shops.shops.items;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.components.shops.IShopItem;

import java.util.HashMap;

@Data
@AllArgsConstructor
public abstract class ShopItem implements IShopItem {

    private final int id;
    private final String store;
    private final String itemKey;
    private final int order;
    private final HashMap<String, String> itemFlags = new HashMap<>();

}
