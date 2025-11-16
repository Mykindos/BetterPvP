package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_shovel")
@FallbackItem(value = Material.GOLDEN_SHOVEL, keepRecipes = true)
public class BoosterShovel extends Sword {

    public BoosterShovel() {
        super("Booster Shovel", Material.GOLDEN_SHOVEL, ItemRarity.UNCOMMON);
    }
}
