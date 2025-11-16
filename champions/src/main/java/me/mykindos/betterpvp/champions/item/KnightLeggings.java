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
@ItemKey("champions:knight_leggings")
@FallbackItem(value = Material.IRON_LEGGINGS, keepRecipes = true)
public class KnightLeggings extends ArmorItem {
    @Inject
    private KnightLeggings(Champions champions) {
        super(champions, "Knight Leggings", ItemStack.of(Material.IRON_LEGGINGS), ItemRarity.COMMON);
    }
} 