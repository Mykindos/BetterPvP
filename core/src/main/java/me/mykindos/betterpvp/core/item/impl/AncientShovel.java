package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_shovel")
@FallbackItem(value = Material.NETHERITE_SHOVEL, keepRecipes = true)
public class AncientShovel extends Sword {

    public AncientShovel() {
        super("Ancient Shovel", Material.NETHERITE_SHOVEL, ItemRarity.UNCOMMON);
    }
}
