package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:crude_axe")
@FallbackItem(value = Material.STONE_AXE, keepRecipes = true)
public class CrudeAxe extends WeaponItem {

    @Inject
    private CrudeAxe(Champions champions) {
        super(champions, "Crude Axe", ItemStack.of(Material.STONE_AXE), ItemRarity.COMMON);
    }

}
