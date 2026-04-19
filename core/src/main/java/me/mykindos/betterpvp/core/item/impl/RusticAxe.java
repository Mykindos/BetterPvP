package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:rustic_axe")
@FallbackItem(value = Material.WOODEN_AXE, keepRecipes = true)
public class RusticAxe extends VanillaItem {

    public RusticAxe() {
        super("Rustic Axe", Material.WOODEN_AXE, ItemRarity.COMMON);
    }
}
