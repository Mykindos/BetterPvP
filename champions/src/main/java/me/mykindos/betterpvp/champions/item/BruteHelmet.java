package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:brute_helmet")
@FallbackItem(value = Material.DIAMOND_HELMET, keepRecipes = true)
public class BruteHelmet extends ArmorItem {
    @Inject
    private BruteHelmet(Champions champions) {
        super(champions, "Brute Helmet", ItemStack.of(Material.DIAMOND_HELMET), ItemRarity.COMMON);
    }
} 