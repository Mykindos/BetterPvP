package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class StandardAxe extends WeaponItem {

    @Inject
    private StandardAxe(Champions champions) {
        super(champions, "Standard Axe", ItemStack.of(Material.IRON_AXE), ItemRarity.COMMON);
    }

}
