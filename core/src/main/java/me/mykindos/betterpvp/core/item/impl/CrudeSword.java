package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_sword")
@FallbackItem(value = Material.STONE_SWORD, keepRecipes = true)
public class CrudeSword extends Sword {

    public CrudeSword() {
        super("Crude Sword", Material.STONE_SWORD, ItemRarity.COMMON);
    }
}
