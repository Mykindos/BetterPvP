package me.mykindos.betterpvp.core.utilities.model;

import org.bukkit.Material;

public interface IItemDisplay {
    Material getMaterial();
    int getCustomModelData();
    boolean isGlowing();
}
