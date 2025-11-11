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
@ItemKey("core:standard_axe")
@FallbackItem(Material.IRON_AXE)
public class StandardAxe extends WeaponItem {

    @Inject
    private StandardAxe(Champions champions) {
        super(champions, "Standard Axe", ItemStack.of(Material.IRON_AXE), ItemRarity.COMMON);
    }

}
