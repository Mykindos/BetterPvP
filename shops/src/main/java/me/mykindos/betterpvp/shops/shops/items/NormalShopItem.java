package me.mykindos.betterpvp.shops.shops.items;

import org.bukkit.Material;

public class NormalShopItem extends ShopItem {

    private final int buyPrice;
    private final int sellPrice;

    public NormalShopItem(String store, String itemName, Material material, int slot, int page, byte data, int buyPrice, int sellPrice) {
        super(store, itemName, material, slot, page, data);
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    @Override
    public int getBuyPrice() {
        return buyPrice;
    }

    @Override
    public int getSellPrice() {
        return sellPrice;
    }


}
