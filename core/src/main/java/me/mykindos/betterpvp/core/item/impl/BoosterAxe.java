package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_axe")
@FallbackItem(value = Material.GOLDEN_AXE, keepRecipes = true)
public class BoosterAxe extends VanillaItem {

    public BoosterAxe() {
        super("Booster Axe", Material.GOLDEN_AXE, ItemRarity.UNCOMMON);
    }
}
