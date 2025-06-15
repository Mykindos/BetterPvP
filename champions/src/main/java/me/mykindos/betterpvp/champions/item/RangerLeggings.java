package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class RangerLeggings extends ArmorItem {
    @Inject
    private RangerLeggings(Champions champions) {
        super(champions, "Ranger Leggings", ItemStack.of(Material.CHAINMAIL_LEGGINGS), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
