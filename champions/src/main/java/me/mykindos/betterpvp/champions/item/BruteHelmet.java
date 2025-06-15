package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class BruteHelmet extends ArmorItem {
    @Inject
    private BruteHelmet(Champions champions) {
        super(champions, "Brute Helmet", ItemStack.of(Material.DIAMOND_HELMET), ItemRarity.COMMON);
    }
} 