package me.mykindos.betterpvp.core.recipe.minecraft;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A simple implementation of Recipe that wraps a Minecraft recipe.
 * This is used for Minecraft recipes that don't have a direct mapping to our custom recipe types.
 */
public class SimpleMinecraftCraftingRecipe implements CraftingRecipe {
    
    private final BaseItem result;
    private final org.bukkit.inventory.Recipe minecraftRecipe;
    private final MinecraftRecipeAdapter adapter;
    private final ItemFactory itemFactory;
    
    /**
     * Creates a new SimpleMinecraftRecipe.
     * 
     * @param result The result of the recipe
     * @param minecraftRecipe The Minecraft recipe to wrap
     * @param adapter The adapter used for recipe matching
     */
    public SimpleMinecraftCraftingRecipe(BaseItem result, org.bukkit.inventory.Recipe minecraftRecipe, MinecraftRecipeAdapter adapter, ItemFactory itemFactory) {
        this.result = result;
        this.minecraftRecipe = minecraftRecipe;
        this.adapter = adapter;
        this.itemFactory = itemFactory;
    }
    
    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Delegate to the adapter for matching
        Optional<org.bukkit.inventory.Recipe> matchingRecipe = adapter.findMatchingRecipe(items);
        return matchingRecipe.isPresent() && matchingRecipe.get().equals(minecraftRecipe);
    }
    
    @Override
    public @NotNull BaseItem getPrimaryResult() {
        return result;
    }

    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return itemFactory.create(result);
    }

    @Override
    public @NotNull RecipeType getType() {
        if (minecraftRecipe instanceof ShapedRecipe) {
            return RecipeType.SHAPED_CRAFTING;
        } else if (minecraftRecipe instanceof ShapelessRecipe) {
            return RecipeType.SHAPELESS_CRAFTING;
        } else {
            return RecipeType.CUSTOM;
        }
    }
    
    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        // This is a simplified implementation that doesn't provide actual ingredients
        return Collections.emptyMap();
    }
    
    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        // For Minecraft recipes, we'll consume one item from each non-empty slot
        List<Integer> consumedSlots = new ArrayList<>();
        
        for (Map.Entry<Integer, ItemInstance> entry : new HashMap<>(ingredients).entrySet()) {
            if (entry.getValue() != null) {
                ItemInstance instance = entry.getValue();
                ItemStack stack = instance.createItemStack();
                
                if (stack.getAmount() <= 1) {
                    ingredients.remove(entry.getKey());
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                    final ItemInstance newInstance = itemFactory.fromItemStack(stack).orElseThrow();
                    ingredients.put(entry.getKey(), newInstance);
                }
                
                consumedSlots.add(entry.getKey());
            }
        }
        
        return consumedSlots;
    }
} 