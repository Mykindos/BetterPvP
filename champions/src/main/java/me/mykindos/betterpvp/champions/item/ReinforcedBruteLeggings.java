package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

@Singleton
@ItemKey("champions:reinforced_brute_leggings")
@FallbackItem(value = Material.DIAMOND_LEGGINGS, keepRecipes = true)
public class ReinforcedBruteLeggings extends ArmorItem {
    @Inject
    private ReinforcedBruteLeggings(Champions champions) {
        super(champions, "Reinforced Brute Leggings", Item.builder(Material.DIAMOND_LEGGINGS)
                .data(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(TrimMaterial.IRON, TrimPattern.SILENCE)).build())
                .build(), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.BRUTE));
    }
} 