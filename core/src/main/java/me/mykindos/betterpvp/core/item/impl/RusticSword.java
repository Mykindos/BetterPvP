package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:rustic_sword")
@FallbackItem(value = Material.WOODEN_SWORD, keepRecipes = true)
public class RusticSword extends Sword {

    public RusticSword() {
        super("Rustic Sword", Material.WOODEN_SWORD, ItemRarity.COMMON);
    }
}
