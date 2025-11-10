package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:crude_axe")
@FallbackItem(value = Material.STONE_AXE, keepRecipes = true)
public class CrudeAxe extends Sword {

    public CrudeAxe() {
        super("Crude Axe", Material.STONE_AXE, ItemRarity.COMMON);
    }
}
