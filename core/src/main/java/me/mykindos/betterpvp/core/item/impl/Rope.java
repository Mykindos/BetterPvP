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
@ItemKey("core:rope")
public class Rope extends BaseItem {

    private transient boolean registered;

    @Inject
    private Rope() {
        super("Rope", Item.model("rope", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem string = itemFactory.getFallbackItem(Material.STRING);
        final RecipeIngredient stringIngredient = new RecipeIngredient(string, 1);
        final Map<Integer, RecipeIngredient> ingredients = Map.of(
                0, stringIngredient,
                1, stringIngredient);
        final ShapelessCraftingRecipe recipe = new ShapelessCraftingRecipe(
                this,
                ingredients,
                itemFactory,
                false
        );
        registry.registerRecipe(new NamespacedKey("core", "rope"), recipe);
    }

}
