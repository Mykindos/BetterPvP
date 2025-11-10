package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:rustic_shovel")
@FallbackItem(value = Material.WOODEN_SHOVEL, keepRecipes = true)
public class RusticShovel extends Sword {

    public RusticShovel() {
        super("Rustic Shovel", Material.WOODEN_SHOVEL, ItemRarity.COMMON);
    }
}
