package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;

public interface IShopkeeper {

    CraftEntity getEntity();

    String getShopkeeperName();

}
