package me.mykindos.betterpvp.shops.shops.items;

import org.bukkit.Material;

public interface IShopItem {

    int getBuyPrice();

    int getSellPrice();

    String getStore();
    String getItemName();
    Material getMaterial();

    int getSlot();
    int getPage();

    byte getData();


}
