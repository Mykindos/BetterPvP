package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry for managing imbuement recipes.
 * Handles registration and lookup of recipes for imbuement crafting.
 */
@CustomLog
@Singleton
public class ImbuementRecipeRegistry implements RecipeRegistry<ImbuementRecipe> {
    
    private final Map<NamespacedKey, ImbuementRecipe> recipes = new HashMap<>();
    private final RecipeResolver<ImbuementRecipe> resolver = new RecipeResolver<>(this);

    @Inject
    private ImbuementRecipeRegistry(RecipeRegistries registries) {
        registries.register(new NamespacedKey("betterpvp", "imbuement"), this);
    }

    /**
     * Registers a new imbuement recipe.
     * @param key The key to register the recipe under
     * @param recipe The recipe to register
     */
    @Override
    public void registerRecipe(NamespacedKey key, @NotNull ImbuementRecipe recipe) {
        if (recipes.containsKey(key)) {
            log.warn("Recipe with key {} is already registered, overwriting", key).submit();
        }

        // Check for duplicate recipes for standard recipes only
        recipes.put(key, recipe);
        
        // Log registration differently based on recipe type
        log.info("Registered custom imbuement recipe: {}", key).submit();
    }

    @Override
    public RecipeResolver<ImbuementRecipe> getResolver() {
        return resolver;
    }

    /**
     * Finds an imbuement recipe that matches the given items.
     * @param items Map of slot indices to ItemStacks
     * @return The matching recipe if found, empty otherwise
     */
    public @NotNull Optional<ImbuementRecipe> findRecipe(@NotNull Map<Integer, ItemStack> items) {
        return recipes.values().stream()
                .filter(recipe -> recipe.matches(items))
                .findFirst();
    }
    
    /**
     * Checks if an imbuement recipe exists for the given items.
     * @param items Map of slot indices to ItemStacks
     * @return true if a recipe exists, false otherwise
     */
    public boolean hasRecipe(@NotNull Map<Integer, ItemStack> items) {
        return findRecipe(items).isPresent();
    }
    
    /**
     * Gets all registered imbuement recipes.
     * @return An unmodifiable list of all recipes
     */
    public @NotNull Set<ImbuementRecipe> getRecipes() {
        return Set.copyOf(recipes.values());
    }
    
    /**
     * Gets all recipes that produce a specific result.
     * @param result The result item to search for
     * @return A list of recipes that produce the result
     */
    public @NotNull List<ImbuementRecipe> getRecipesForResult(@NotNull BaseItem result) {
        return recipes.values().stream()
                .filter(recipe -> {
                    if (recipe instanceof StandardImbuementRecipe standardRecipe) {
                        return standardRecipe.getPrimaryResult().getPrimaryResult().equals(result);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
} 