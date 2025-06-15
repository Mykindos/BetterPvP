package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class WarlockBoots extends ArmorItem {
    @Inject
    private WarlockBoots(Champions champions) {
        super(champions, "Warlock Boots", ItemStack.of(Material.NETHERITE_BOOTS), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
