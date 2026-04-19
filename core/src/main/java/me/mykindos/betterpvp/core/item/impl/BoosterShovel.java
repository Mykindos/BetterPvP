package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_shovel")
@FallbackItem(value = Material.GOLDEN_SHOVEL, keepRecipes = true)
public class BoosterShovel extends VanillaItem {

    public BoosterShovel() {
        super("Booster Shovel", Material.GOLDEN_SHOVEL, ItemRarity.UNCOMMON);
    }
}
