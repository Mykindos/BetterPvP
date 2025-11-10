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
@ItemKey("champions:assassin_helmet")
@FallbackItem(value = Material.LEATHER_HELMET, keepRecipes = true)
public class AssassinHelmet extends ArmorItem {
    @Inject
    private AssassinHelmet(Champions champions) {
        super(champions, "Assassin Helmet", ItemStack.of(Material.LEATHER_HELMET), ItemRarity.COMMON);
    }
} 