package me.mykindos.betterpvp.shops.shops.items;

import lombok.Data;
import org.bukkit.Material;

@Data
public abstract class ShopItem implements IShopItem {

    private final String store;
    private final String itemName;
    private final Material material;
    private final int slot;
    private final int page;
    private final byte data;

}
