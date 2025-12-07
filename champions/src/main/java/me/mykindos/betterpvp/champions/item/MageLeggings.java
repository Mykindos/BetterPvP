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
@ItemKey("champions:mage_leggings")
@FallbackItem(value = Material.GOLDEN_LEGGINGS, keepRecipes = true)
public class MageLeggings extends ArmorItem {
    @Inject
    private MageLeggings(Champions champions) {
        super(champions, "Mage Leggings", ItemStack.of(Material.GOLDEN_LEGGINGS), ItemRarity.COMMON);
    }
} 