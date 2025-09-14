package me.mykindos.betterpvp.core.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry for managing imbuement recipes.
 * Handles registration and lookup of recipes for imbuement crafting.
 */
@CustomLog
@Singleton
public class ImbuementRecipeRegistry implements RecipeRegistry<ImbuementRecipe> {
    
    private final List<ImbuementRecipe> recipes = new ArrayList<>();
    private final RecipeResolver<ImbuementRecipe> resolver = new RecipeResolver<>(this);

    @Inject
    private ImbuementRecipeRegistry(RecipeRegistries registries) {
        registries.register(this);
    }

    /**
     * Registers a new imbuement recipe.
     * @param recipe The recipe to register
     */
    @Override
    public void registerRecipe(@NotNull ImbuementRecipe recipe) {
        // Check for duplicate recipes for standard recipes only
        recipes.add(recipe);
        
        // Log registration differently based on recipe type
        log.info("Registered custom imbuement recipe: {}", recipe.getClass().getSimpleName()).submit();
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
        return recipes.stream()
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
    public @NotNull List<ImbuementRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }
    
    /**
     * Gets all recipes that produce a specific result.
     * @param result The result item to search for
     * @return A list of recipes that produce the result
     */
    public @NotNull List<ImbuementRecipe> getRecipesForResult(@NotNull BaseItem result) {
        return recipes.stream()
                .filter(recipe -> {
                    if (recipe instanceof StandardImbuementRecipe standardRecipe) {
                        return standardRecipe.getPrimaryResult().getPrimaryResult().equals(result);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
} 