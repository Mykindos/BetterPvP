package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:standard_hoe")
@FallbackItem(value = Material.IRON_HOE, keepRecipes = true)
public class StandardHoe extends Sword {

    public StandardHoe() {
        super("Standard Hoe", Material.IRON_HOE, ItemRarity.COMMON);
    }
}
