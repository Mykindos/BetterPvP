package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:power_sword")
@FallbackItem(value = Material.DIAMOND_SWORD, keepRecipes = true)
public class PowerSword extends Sword {

    public PowerSword() {
        super("Power Sword", Material.DIAMOND_SWORD, ItemRarity.UNCOMMON);
    }
}
