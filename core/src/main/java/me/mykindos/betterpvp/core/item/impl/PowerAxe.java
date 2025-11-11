package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:power_axe")
@FallbackItem(value = Material.DIAMOND_AXE, keepRecipes = true)
public class PowerAxe extends Sword {

    public PowerAxe() {
        super("Power Axe", Material.DIAMOND_AXE, ItemRarity.UNCOMMON);
    }
}
