package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_pickaxe")
@FallbackItem(value = Material.GOLDEN_PICKAXE, keepRecipes = true)
public class BoosterPickaxe extends Sword {

    public BoosterPickaxe() {
        super("Booster Pickaxe", Material.GOLDEN_PICKAXE, ItemRarity.UNCOMMON);
    }
}
