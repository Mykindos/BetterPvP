package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_shovel")
@FallbackItem(value = Material.NETHERITE_SHOVEL, keepRecipes = true)
public class AncientShovel extends VanillaItem {

    public AncientShovel() {
        super("Ancient Shovel", Material.NETHERITE_SHOVEL, ItemRarity.RARE);
    }
}
