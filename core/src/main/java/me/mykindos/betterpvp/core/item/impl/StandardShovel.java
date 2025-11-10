package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:standard_shovel")
@FallbackItem(value = Material.IRON_SHOVEL, keepRecipes = true)
public class StandardShovel extends Sword {

    public StandardShovel() {
        super("Standard Shovel", Material.IRON_SHOVEL, ItemRarity.COMMON);
    }
}
