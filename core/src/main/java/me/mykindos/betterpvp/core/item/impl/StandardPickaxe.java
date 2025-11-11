package me.mykindos.betterpvp.core.item.impl;

import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@ItemKey("core:standard_pickaxe")
@FallbackItem(value = Material.IRON_PICKAXE, keepRecipes = true)
public class StandardPickaxe extends Sword {

    public StandardPickaxe() {
        super("Standard Pickaxe", Material.IRON_PICKAXE, ItemRarity.COMMON);
    }
}
