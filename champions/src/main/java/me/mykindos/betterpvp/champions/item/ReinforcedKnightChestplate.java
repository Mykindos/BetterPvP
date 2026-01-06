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
@ItemKey("champions:reinforced_knight_chestplate")
@FallbackItem(value = Material.IRON_CHESTPLATE, keepRecipes = true)
public class ReinforcedKnightChestplate extends ArmorItem {
    @Inject
    private ReinforcedKnightChestplate(Champions champions) {
        super(champions, "Reinforced Knight Chestplate", Item.builder(Material.IRON_CHESTPLATE)
                .data(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(TrimMaterial.IRON, TrimPattern.SILENCE)).build())
                .build(), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.KNIGHT));
    }
} 