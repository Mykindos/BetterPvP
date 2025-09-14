package me.mykindos.betterpvp.core.recipe.smelting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import net.kyori.adventure.key.Namespaced;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry specifically for managing smelting recipes.
 * Handles registration, validation, and provides utilities for determining smeltable items.
 */
@CustomLog
@Singleton
public class SmeltingRecipeRegistry implements RecipeRegistry<SmeltingRecipe> {
    
    private final Map<NamespacedKey, SmeltingRecipe> smeltingRecipes = new HashMap<>();
    private final Set<BaseItem> smeltableItems = new HashSet<>();
    private final RecipeResolver<SmeltingRecipe> resolver = new RecipeResolver<>(this);

    @Inject
    private SmeltingRecipeRegistry(RecipeRegistries registries) {
        registries.register(new NamespacedKey("betterpvp", "smelting"), this);
    }
    
    /**
     * Registers a new smelting recipe.
     * Validates that no duplicate recipe exists with the same ingredient types.
     *
     * @param key The key to register the recipe under
     * @param recipe The smelting recipe to register
     * @throws IllegalArgumentException if a recipe with the same ingredient types already exists
     */
    @Override
    public void registerRecipe(NamespacedKey key, @NotNull SmeltingRecipe recipe) {
        if (smeltingRecipes.containsKey(key)) {
            log.warn("Recipe with key {} is already registered, overwriting", key).submit();
        }

        // Check for duplicate recipes (same ingredient types, ignoring quantities)
        Set<BaseItem> newIngredientTypes = recipe.getIngredientTypes();
        
        // Add to our smelting-specific collections
        smeltingRecipes.put(key, recipe);
        smeltableItems.addAll(recipe.getIngredientTypes());
        
        log.info("Registered smelting recipe with ingredients: {} -> {}", 
                newIngredientTypes, recipe.getSmeltingResult().getPrimaryResult().getName()).submit();
    }
    
    /**
     * Checks if the given item can be used in smelting recipes.
     * An item is smeltable if it appears as an ingredient in any registered smelting recipe.
     * 
     * @param item The item to check
     * @return true if the item can be smelted, false otherwise
     */
    public boolean isSmeltable(@NotNull BaseItem item) {
        return smeltableItems.contains(item);
    }
    
    /**
     * Gets all items that can be used in smelting recipes.
     * @return An unmodifiable set of all smeltable items
     */
    public @NotNull Set<BaseItem> getSmeltableItems() {
        return Collections.unmodifiableSet(smeltableItems);
    }
    
    /**
     * Gets all registered smelting recipes.
     * @return An unmodifiable set of all smelting recipes
     */
    public @NotNull Set<SmeltingRecipe> getRecipes() {
        return Set.copyOf(smeltingRecipes.values());
    }

    @Override
    public RecipeResolver<SmeltingRecipe> getResolver() {
        return resolver;
    }

    /**
     * Gets all smelting recipes that use the specified item as an ingredient.
     * @param item The item to search for
     * @return A list of recipes that use the item
     */
    public @NotNull List<SmeltingRecipe> getRecipesUsingItem(@NotNull BaseItem item) {
        return smeltingRecipes.values().stream()
                .filter(recipe -> recipe.getIngredientTypes().contains(item))
                .toList();
    }
} 