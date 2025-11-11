package me.mykindos.betterpvp.core.anvil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry for managing anvil recipes.
 * Handles registration and lookup of recipes for anvil crafting.
 */
@CustomLog
@Singleton
public class AnvilRecipeRegistry implements RecipeRegistry<AnvilRecipe> {
    
    private final Map<NamespacedKey, AnvilRecipe> recipes = new HashMap<>();
    private final RecipeResolver<AnvilRecipe> resolver = new RecipeResolver<>(this);

    @Inject
    private AnvilRecipeRegistry(RecipeRegistries registries) {
        registries.register(new NamespacedKey("betterpvp", "anvil"), this);
    }

    @Override
    public void registerRecipe(@NotNull NamespacedKey key, @NotNull AnvilRecipe recipe) {
        if (recipes.containsKey(key)) {
            log.warn("Recipe with key {} is already registered, overwriting", key).submit();
        }

        recipes.put(key, recipe);
        log.info("Registered anvil recipe for {} requiring {} hammer swings with {} ingredients",
                recipe.getResult().getPrimaryResult().getClass().getSimpleName(),
                recipe.getHammerSwings(),
                recipe.getIngredients().size()).submit();
    }

    @Override
    public RecipeResolver<AnvilRecipe> getResolver() {
        return resolver;
    }

    /**
     * Finds an anvil recipe that matches the given items.
     * @param items Map of slot indices to ItemStacks
     * @return The matching recipe if found, empty otherwise
     */
    public @NotNull Optional<AnvilRecipe> findRecipe(@NotNull Map<Integer, ItemStack> items) {
        return recipes.values().stream()
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
        return recipes.values().stream()
                .filter(recipe -> recipe.getIngredientTypes().contains(ingredient))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds all anvil recipes that produce the given result.
     * @param result The result base item
     * @return List of recipes that produce this result
     */
    public @NotNull List<AnvilRecipe> findRecipesWithResult(@NotNull BaseItem result) {
        return recipes.values().stream()
                .filter(recipe -> recipe.getResult().getPrimaryResult().equals(result) ||
                                recipe.getResult().getSecondaryResults().contains(result))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all registered anvil recipes.
     * @return An unmodifiable list of all recipes
     */
    public @NotNull Set<AnvilRecipe> getRecipes() {
        return Set.copyOf(recipes.values());
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