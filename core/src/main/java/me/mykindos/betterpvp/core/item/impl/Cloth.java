package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Map;

@Singleton
@ItemKey("core:cloth")
public class Cloth extends BaseItem {
    private transient boolean registered;

    @Inject
    private Cloth() {
        super("Cloth", Item.model("cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        final BaseItem wool = itemFactory.getFallbackItem(Material.WHITE_WOOL);
        final RecipeIngredient woolIngredient = new RecipeIngredient(wool, 1);
        final Map<Integer, RecipeIngredient> ingredients = Map.of(
                0, woolIngredient,
                1, woolIngredient,
                2, woolIngredient
        );

        final ShapelessCraftingRecipe recipe = new ShapelessCraftingRecipe(
                this, ingredients, itemFactory, false
        );

        registry.registerRecipe(new NamespacedKey("core", "cloth"), recipe);
    }

}
