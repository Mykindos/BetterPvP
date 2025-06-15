package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class WarlockChestplate extends ArmorItem {
    @Inject
    private WarlockChestplate(Champions champions) {
        super(champions, "Warlock Chestplate", ItemStack.of(Material.NETHERITE_CHESTPLATE), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
