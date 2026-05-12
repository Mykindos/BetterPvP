package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Singleton
@ItemKey("core:chest")
@FallbackItem(value = Material.CHEST, keepRecipes = true)
public class Chest extends BaseItem {

    private static final List<Material> WOOD_PLANKS = List.of(
            //Material.OAK_PLANKS,
            Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS,
            Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS,
            Material.CHERRY_PLANKS,
            Material.BAMBOO_PLANKS,
            Material.CRIMSON_PLANKS,
            Material.WARPED_PLANKS,
            Material.PALE_OAK_PLANKS
    );

    private transient boolean registered;

    public Chest() {
        super("Chest", ItemStack.of(Material.CHEST), ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipes(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        for (Material planks : WOOD_PLANKS) {
            final BaseItem ingredient = itemFactory.getFallbackItem(planks);
            final ShapedCraftingRecipe recipe = new ShapedCraftingRecipe.Builder(
                    this,
                    new String[]{"PPP", "P P", "PPP"},
                    itemFactory
            )
                    .setIngredient('P', new RecipeIngredient(ingredient, 1))
                    .build();
            final String planksName = planks.name().toLowerCase();
            registry.registerRecipe(new NamespacedKey("core", "chest_from_" + planksName), recipe);
        }
    }
}


