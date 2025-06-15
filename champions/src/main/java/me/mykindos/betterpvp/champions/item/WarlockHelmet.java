package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class WarlockHelmet extends ArmorItem {
    @Inject
    private WarlockHelmet(Champions champions) {
        super(champions, "Warlock Helmet", ItemStack.of(Material.NETHERITE_HELMET), ItemRarity.COMMON);
        // TODO: Add shaped recipe
    }
}
