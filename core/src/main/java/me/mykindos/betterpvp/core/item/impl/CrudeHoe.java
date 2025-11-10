package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_hoe")
@FallbackItem(value = Material.STONE_HOE, keepRecipes = true)
public class CrudeHoe extends Sword {

    public CrudeHoe() {
        super("Crude Hoe", Material.STONE_HOE, ItemRarity.COMMON);
    }
}
