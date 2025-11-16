package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:power_hoe")
@FallbackItem(value = Material.DIAMOND_HOE, keepRecipes = true)
public class PowerHoe extends Sword {

    public PowerHoe() {
        super("Power Hoe", Material.DIAMOND_HOE, ItemRarity.UNCOMMON);
    }
}
