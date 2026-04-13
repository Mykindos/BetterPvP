package me.mykindos.betterpvp.shops.shops.items;

public class NormalShopItem extends ShopItem {

    private final int buyPrice;
    private final int sellPrice;

    public NormalShopItem(int id, String store, String itemKey, int order, int buyPrice, int sellPrice) {
        super(id, store, itemKey, order);
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
