package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_hoe")
@FallbackItem(value = Material.GOLDEN_HOE, keepRecipes = true)
public class BoosterHoe extends Sword {

    public BoosterHoe() {
        super("Booster Hoe", Material.GOLDEN_HOE, ItemRarity.UNCOMMON);
    }
}
