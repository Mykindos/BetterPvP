package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_pickaxe")
@FallbackItem(value = Material.STONE_PICKAXE, keepRecipes = true)
public class CrudePickaxe extends Sword {

    public CrudePickaxe() {
        super("Crude Pickaxe", Material.STONE_PICKAXE, ItemRarity.COMMON);
    }
}
