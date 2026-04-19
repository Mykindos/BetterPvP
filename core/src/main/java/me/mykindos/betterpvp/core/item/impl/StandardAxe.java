package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:standard_axe")
@FallbackItem(value = Material.IRON_AXE, keepRecipes = true)
public class StandardAxe extends VanillaItem {

    public StandardAxe() {
        super("Standard Axe", Material.IRON_AXE, ItemRarity.COMMON);
    }
}
