package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class BruteLeggings extends ArmorItem {
    @Inject
    private BruteLeggings(Champions champions) {
        super(champions, "Brute Leggings", ItemStack.of(Material.DIAMOND_LEGGINGS), ItemRarity.COMMON);
    }
} 