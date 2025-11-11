package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:rustic_hoe")
@FallbackItem(value = Material.WOODEN_HOE, keepRecipes = true)
public class RusticHoe extends Sword {

    public RusticHoe() {
        super("Rustic Hoe", Material.WOODEN_HOE, ItemRarity.COMMON);
    }
}
