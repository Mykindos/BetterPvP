package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class WarlockLeggings extends ArmorItem {
    @Inject
    private WarlockLeggings(Champions champions) {
        super(champions, "Warlock Leggings", ItemStack.of(Material.NETHERITE_LEGGINGS), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
