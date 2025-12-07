package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:power_pickaxe")
@FallbackItem(value = Material.DIAMOND_PICKAXE, keepRecipes = true)
public class PowerPickaxe extends Sword {

    public PowerPickaxe() {
        super("Power Pickaxe", Material.DIAMOND_PICKAXE, ItemRarity.UNCOMMON);
    }
}
