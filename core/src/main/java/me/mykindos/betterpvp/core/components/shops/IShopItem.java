package me.mykindos.betterpvp.core.components.shops;

import org.bukkit.Material;

public interface IShopItem {

    int getId();
    int getBuyPrice();

    int getSellPrice();

    String getStore();
    String getItemName();
    Material getMaterial();
    int getModelData();
    int getSlot();
    int getPage();
    int getAmount();




}
