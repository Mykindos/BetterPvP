package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_axe")
@FallbackItem(value = Material.NETHERITE_AXE, keepRecipes = true)
public class AncientAxe extends Sword {

    public AncientAxe() {
        super("Ancient Axe", Material.NETHERITE_AXE, ItemRarity.UNCOMMON);
    }
}
