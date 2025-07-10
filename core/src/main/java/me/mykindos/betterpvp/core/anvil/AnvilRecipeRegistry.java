package me.mykindos.betterpvp.core.anvil;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry for managing anvil recipes.
 * Handles registration and lookup of recipes for anvil crafting.
 */
@CustomLog
@Singleton
public class AnvilRecipeRegistry {
    
    private final List<AnvilRecipe> recipes = new ArrayList<>();
    
    /**
     * Registers a new anvil recipe.
     * @param recipe The recipe to register
     */
    public void registerRecipe(@NotNull AnvilRecipe recipe) {
        recipes.add(recipe);
        log.info("Registered anvil recipe for {} requiring {} hammer swings with {} ingredients",
                recipe.getResult().getPrimaryResult().getClass().getSimpleName(),
                recipe.getHammerSwings(),
                recipe.getIngredients().size()).submit();
    }
    
    /**
     * Finds an anvil recipe that matches the given items.
     * @param items Map of slot indices to ItemStacks
     * @return The matching recipe if found, empty otherwise
     */
    public @NotNull Optional<AnvilRecipe> findRecipe(@NotNull Map<Integer, ItemStack> items) {
        return recipes.stream()
                .filter(recipe -> recipe.matches(items))
                .findFirst();
    }
    
    /**
     * Checks if an anvil recipe exists for the given items.
     * @param items Map of slot indices to ItemStacks
     * @return true if a recipe exists, false otherwise
     */
    public boolean hasRecipe(@NotNull Map<Integer, ItemStack> items) {
        return findRecipe(items).isPresent();
    }
    
    /**
     * Finds all anvil recipes that use the given ingredient.
     * @param ingredient The ingredient base item
     * @return List of recipes that use this ingredient
     */
    public @NotNull List<AnvilRecipe> findRecipesWithIngredient(@NotNull BaseItem ingredient) {
        return recipes.stream()
                .filter(recipe -> recipe.getIngredientTypes().contains(ingredient))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds all anvil recipes that produce the given result.
     * @param result The result base item
     * @return List of recipes that produce this result
     */
    public @NotNull List<AnvilRecipe> findRecipesWithResult(@NotNull BaseItem result) {
        return recipes.stream()
                .filter(recipe -> recipe.getResult().getPrimaryResult().equals(result) ||
                                recipe.getResult().getSecondaryResults().contains(result))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds anvil recipes that require a specific number of hammer swings.
     * @param hammerSwings The required hammer swing count
     * @return List of recipes with the specified hammer swing requirement
     */
    public @NotNull List<AnvilRecipe> findRecipesByHammerSwings(int hammerSwings) {
        return recipes.stream()
                .filter(recipe -> recipe.getHammerSwings() == hammerSwings)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all registered anvil recipes.
     * @return An unmodifiable list of all recipes
     */
    public @NotNull List<AnvilRecipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }
    
    /**
     * Gets the count of registered recipes.
     * @return The number of registered recipes
     */
    public int getRecipeCount() {
        return recipes.size();
    }
    
    /**
     * Clears all registered recipes.
     * This method is primarily for testing purposes.
     */
    public void clear() {
        recipes.clear();
        log.info("Cleared all anvil recipes").submit();
    }
} 