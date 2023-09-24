package me.mykindos.betterpvp.core.components.shops;

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
