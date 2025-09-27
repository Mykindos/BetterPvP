package me.mykindos.betterpvp.core.recipe.minecraft;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import net.kyori.adventure.key.Namespaced;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter for working with Minecraft's built-in recipes.
 * Handles finding, matching, and converting Minecraft recipes to our custom recipe format.
 */
@CustomLog
@Singleton
public class MinecraftCraftingRecipeAdapter {

    private final ItemFactory itemFactory;

    @Inject
    private MinecraftCraftingRecipeAdapter(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public Map<NamespacedKey, CraftingRecipe> getRecipes() {
        Map<NamespacedKey, CraftingRecipe> recipes = new HashMap<>();
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof org.bukkit.inventory.CraftingRecipe craftingRecipe) {
                CraftingRecipe convertedRecipe = convertToCustomRecipe(craftingRecipe);
                if (convertedRecipe != null) {
                    recipes.put(craftingRecipe.getKey(), convertedRecipe);
                }
            }
        });
        return recipes;
    }

    /**
     * Converts a Minecraft recipe to our recipe format.
     * 
     * @param minecraftRecipe The Minecraft recipe to convert
     * @return Our recipe format
     */
    public CraftingRecipe convertToCustomRecipe(org.bukkit.inventory.Recipe minecraftRecipe) {
        ItemStack resultStack = minecraftRecipe.getResult();
        if (minecraftRecipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
            return convertShapedRecipe(shapedRecipe, resultStack);
        } else if (minecraftRecipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
            return convertShapelessRecipe(shapelessRecipe, resultStack);
        } else {
            return null; // Unsupported recipe type
        }
    }
    
    /**
     * Converts a Minecraft shaped recipe to our recipe format.
     * 
     * @param shapedRecipe The Minecraft shaped recipe to convert
     * @param result Our result ItemInstance
     * @return Our shaped recipe format
     */
    private ShapedCraftingRecipe convertShapedRecipe(org.bukkit.inventory.ShapedRecipe shapedRecipe, ItemStack result) {
        // Get the recipe shape and choice map
        String[] shape = shapedRecipe.getShape();
        Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
        
        // Create a builder for our shaped recipe
        ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(() -> {
            return itemFactory.fromItemStack(result).orElseThrow();
        }, shape, itemFactory);
        
        // Add the ingredients
        for (Map.Entry<Character, RecipeChoice> entry : choiceMap.entrySet()) {
            char key = entry.getKey();
            RecipeChoice choice = entry.getValue();
            
            if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                // Use the first material as the ingredient
                Material material = materialChoice.getChoices().getFirst();
                ItemStack itemStack = new ItemStack(material);
                ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
                builder.setIngredient(key, new RecipeIngredient(instance.getBaseItem(), 1));
            } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                // Use the first item as the ingredient
                ItemStack item = exactChoice.getChoices().getFirst();
                ItemInstance instance = itemFactory.fromItemStack(item).orElseThrow();
                builder.setIngredient(key, new RecipeIngredient(instance.getBaseItem(), 1));
            }
        }
        
        return builder.build();
    }
    
    /**
     * Converts a Minecraft shapeless recipe to our recipe format.
     * 
     * @param shapelessRecipe The Minecraft shapeless recipe to convert
     * @param result Our result ItemInstance
     * @return Our shapeless recipe format
     */
    private ShapelessCraftingRecipe convertShapelessRecipe(org.bukkit.inventory.ShapelessRecipe shapelessRecipe, ItemStack result) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        
        // Convert each choice to an ingredient
        for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
            if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                // Use the first material as the ingredient
                Material material = materialChoice.getChoices().getFirst();
                ItemStack itemStack = new ItemStack(material);
                ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
                ingredients.add(new RecipeIngredient(instance.getBaseItem(), 1));
            } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                // Use the first item as the ingredient
                ItemStack item = exactChoice.getChoices().getFirst();
                ItemInstance instance = itemFactory.fromItemStack(item).orElseThrow();
                ingredients.add(new RecipeIngredient(instance.getBaseItem(), 1));
            }
        }
        
        Map<Integer, RecipeIngredient> ingredientMap = new HashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            ingredientMap.put(i, ingredients.get(i));
        }
        
        return new ShapelessCraftingRecipe(() -> {
            return itemFactory.fromItemStack(result).orElseThrow();
        }, ingredientMap, itemFactory, false);
    }

    public void registerDefaults(Map<NamespacedKey, CraftingRecipe> craftingRecipes) {
        log.info("Registering default Minecraft crafting recipes").submit();

        long start = System.nanoTime();

        craftingRecipes.putAll(getRecipes());

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;

        log.info("Registered {} default Minecraft crafting recipes in {} ms",
                craftingRecipes.size(), String.format("%.2f", ms)).submit();
    }

}