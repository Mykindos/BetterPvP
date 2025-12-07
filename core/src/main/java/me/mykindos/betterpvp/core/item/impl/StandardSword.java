package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:standard_sword")
@FallbackItem(value = Material.IRON_SWORD, keepRecipes = true)
public class StandardSword extends Sword {

    public StandardSword() {
        super("Standard Sword", Material.IRON_SWORD, ItemRarity.COMMON);
    }
}
