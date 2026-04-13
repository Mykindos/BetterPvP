package me.mykindos.betterpvp.core.components.shops;

public interface IShopItem {

    int getId();
    int getBuyPrice();

    int getSellPrice();

    String getStore();
    String getItemKey();
    int getOrder();

}
