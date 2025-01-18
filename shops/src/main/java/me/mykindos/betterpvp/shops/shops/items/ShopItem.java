package me.mykindos.betterpvp.shops.shops.items;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import org.bukkit.Material;

import java.util.HashMap;

@Data
@AllArgsConstructor
public abstract class ShopItem implements IShopItem {

    private final int id;
    private final String store;
    private final String itemName;
    private final Material material;
    private final int modelData;
    private int slot;
    private final int page;
    private final int amount;
    private final HashMap<String, String> itemFlags = new HashMap<>();

}
