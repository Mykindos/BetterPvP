package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_hoe")
@FallbackItem(value = Material.NETHERITE_HOE, keepRecipes = true)
public class AncientHoe extends Sword {

    public AncientHoe() {
        super("Ancient Hoe", Material.NETHERITE_HOE, ItemRarity.UNCOMMON);
    }
}
