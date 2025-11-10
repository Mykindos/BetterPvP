package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:power_shovel")
@FallbackItem(value = Material.DIAMOND_SHOVEL, keepRecipes = true)
public class PowerShovel extends Sword {

    public PowerShovel() {
        super("Power Shovel", Material.DIAMOND_SHOVEL, ItemRarity.UNCOMMON);
    }
}
