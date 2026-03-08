package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_sword")
@FallbackItem(value = Material.NETHERITE_SWORD, keepRecipes = true)
public class AncientSword extends Sword {

    public AncientSword() {
        super("Ancient Sword", Material.NETHERITE_SWORD, ItemRarity.UNCOMMON);
    }
}
