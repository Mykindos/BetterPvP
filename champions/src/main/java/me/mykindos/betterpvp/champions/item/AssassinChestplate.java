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
@ItemKey("champions:assassin_chestplate")
@FallbackItem(value = Material.LEATHER_CHESTPLATE, keepRecipes = true)
public class AssassinChestplate extends ArmorItem {
    @Inject
    private AssassinChestplate(Champions champions) {
        super(champions, "Assassin Chestplate", ItemStack.of(Material.LEATHER_CHESTPLATE), ItemRarity.COMMON);
    }
} 