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
@ItemKey("champions:mage_boots")
@FallbackItem(value = Material.GOLDEN_BOOTS, keepRecipes = true)
public class MageBoots extends ArmorItem {
    @Inject
    private MageBoots(Champions champions) {
        super(champions, "Mage Boots", ItemStack.of(Material.GOLDEN_BOOTS), ItemRarity.COMMON);
    }
} 