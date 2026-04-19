package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_shovel")
@FallbackItem(value = Material.STONE_SHOVEL, keepRecipes = true)
public class CrudeShovel extends VanillaItem {

    public CrudeShovel() {
        super("Crude Shovel", Material.STONE_SHOVEL, ItemRarity.COMMON);
    }
}
