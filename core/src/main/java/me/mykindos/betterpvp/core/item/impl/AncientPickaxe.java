package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@Singleton
@ItemKey("core:ancient_pickaxe")
@FallbackItem(value = Material.NETHERITE_PICKAXE, keepRecipes = true)
public class AncientPickaxe extends Sword {

    public AncientPickaxe() {
        super("Ancient Pickaxe", Material.NETHERITE_PICKAXE, ItemRarity.UNCOMMON);
    }
}
