package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
@ItemKey("core:torch")
@FallbackItem(value = Material.TORCH, keepRecipes = true)
@CustomLog
public class Torch extends VanillaItem {

    private transient boolean registered;

    @Inject
    private Torch() {
        super("Torch", Material.TORCH, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;

        //the default minecraft:torch recipe uses coal, we need to register the charcoal recipe to match https://minecraft.wiki/w/Torch#Crafting_ingredient

        final BaseItem charcoal = itemFactory.getFallbackItem(Material.CHARCOAL);
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);

        final RecipeIngredient charcoalIngredient = new RecipeIngredient(charcoal, 1);
        final RecipeIngredient stickIngredient = new RecipeIngredient(stick, 1);

        final String[] pattern = new String[] {
                "C",
                "S",
        };

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(
                this,
                instance -> instance.getItemStack().setAmount(4),
                pattern,
                itemFactory
                )
                .setIngredient('S', stickIngredient);
        final ShapedCraftingRecipe charcoalRecipe = builder.setIngredient('C', charcoalIngredient).build();


        registry.registerRecipe(new NamespacedKey("core", "torch_charcoal"), charcoalRecipe);
    }

}
