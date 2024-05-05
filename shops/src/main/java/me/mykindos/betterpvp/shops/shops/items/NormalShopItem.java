package me.mykindos.betterpvp.shops.shops.items;

import org.bukkit.Material;

public class NormalShopItem extends ShopItem {

    private final int buyPrice;
    private final int sellPrice;

    public NormalShopItem(int id, String store, String itemName, Material material, int modelData, int slot, int page, int amount, int buyPrice, int sellPrice) {
        super(id, store, itemName, material, modelData, slot, page, amount);
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
