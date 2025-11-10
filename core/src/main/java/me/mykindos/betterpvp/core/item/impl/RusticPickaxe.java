package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:rustic_pickaxe")
@FallbackItem(value = Material.WOODEN_PICKAXE, keepRecipes = true)
public class RusticPickaxe extends Sword {

    public RusticPickaxe() {
        super("Rustic Pickaxe", Material.WOODEN_PICKAXE, ItemRarity.COMMON);
    }
}
