package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_sword")
@FallbackItem(value = Material.GOLDEN_SWORD, keepRecipes = true)
public class BoosterSword extends Sword {

    public BoosterSword() {
        super("Booster Sword", Material.GOLDEN_SWORD, ItemRarity.UNCOMMON);
    }
}
