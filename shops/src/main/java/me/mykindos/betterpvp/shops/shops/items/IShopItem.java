package me.mykindos.betterpvp.shops.shops.items;

import org.bukkit.Material;

public interface IShopItem {

    int getBuyPrice();

    int getSellPrice();

    String getStore();
    String getItemName();
    Material getMaterial();
    byte getData();
    int getSlot();
    int getPage();
    int getAmount();




}
