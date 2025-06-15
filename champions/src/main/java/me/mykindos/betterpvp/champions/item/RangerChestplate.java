package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class RangerChestplate extends ArmorItem {
    @Inject
    private RangerChestplate(Champions champions) {
        super(champions, "Ranger Chestplate", ItemStack.of(Material.CHAINMAIL_CHESTPLATE), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
