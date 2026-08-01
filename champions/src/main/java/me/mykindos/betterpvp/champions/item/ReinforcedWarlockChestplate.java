package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

@Singleton
@ItemKey("champions:reinforced_warlock_chestplate")
public class ReinforcedWarlockChestplate extends ArmorItem {

    private transient boolean registered;

    @Inject
    private ReinforcedWarlockChestplate(Champions champions) {
        super(champions, translatableName("champions.item.reinforced-warlock-chestplate.name"), Item.builder(Material.NETHERITE_CHESTPLATE)
                .data(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(TrimMaterial.IRON, TrimPattern.HOST)).build())
                .build(), ItemRarity.UNCOMMON);
        addBaseComponent(new RoleArmorComponent(Role.WARLOCK));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem ingot = itemFactory.getFallbackItem(Material.NETHERITE_INGOT);
        final BaseItem block = itemFactory.getFallbackItem(Material.NETHERITE_BLOCK);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, new String[] {
                "I I",
                "IBI",
                "III",
        }, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ingot, 1));
        builder.setIngredient('B', new RecipeIngredient(block, 1));
        registry.registerRecipe(new NamespacedKey("champions", "reinforced_warlock_chestplate"), builder.build());
    }
}
