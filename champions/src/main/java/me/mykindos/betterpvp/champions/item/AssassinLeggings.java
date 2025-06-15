package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class AssassinLeggings extends ArmorItem {
    @Inject
    private AssassinLeggings(Champions champions) {
        super(champions, "Assassin Leggings", ItemStack.of(Material.LEATHER_LEGGINGS), ItemRarity.COMMON);
    }
} 