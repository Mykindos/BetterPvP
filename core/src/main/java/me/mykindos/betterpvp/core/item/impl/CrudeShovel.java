package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_shovel")
@FallbackItem(value = Material.STONE_SHOVEL, keepRecipes = true)
public class CrudeShovel extends Sword {

    public CrudeShovel() {
        super("Crude Shovel", Material.STONE_SHOVEL, ItemRarity.COMMON);
    }
}
