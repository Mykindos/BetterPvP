package me.mykindos.betterpvp.core.metal.casting;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Registry for managing casting mold recipes.
 * Handles registration and lookup of recipes for different casting mold types.
 */
@CustomLog
@Singleton
public class CastingMoldRecipeRegistry {
    
    private final List<CastingMoldRecipe> recipes = new ArrayList<>();
    
    /**
     * Registers a new casting mold recipe.
     * @param recipe The recipe to register
     */
    public void registerRecipe(@NotNull CastingMoldRecipe recipe) {
        recipes.add(recipe);
        log.info("Registered casting mold recipe for {} requiring {} mB with {} accepted alloys",
                recipe.getBaseMold().getClass().getSimpleName(),
                recipe.getRequiredMillibuckets(),
                recipe.getAcceptedAlloys().size()).submit();
    }
    
    /**
     * Finds a casting mold recipe for the given base mold.
     * @param baseMold The base casting mold
     * @return The recipe if found, empty otherwise
     */
    public @NotNull Optional<CastingMoldRecipe> findRecipe(@NotNull BaseItem baseMold) {
        return recipes.stream()
                .filter(recipe -> recipe.matches(baseMold))
                .findFirst();
    }
    
    /**
     * Checks if a casting mold recipe exists for the given base mold.
     * @param baseMold The base casting mold
     * @return true if a recipe exists, false otherwise
     */
    public boolean hasRecipe(@NotNull BaseItem baseMold) {
        return findRecipe(baseMold).isPresent();
    }
    
    /**
     * Finds a casting mold recipe that can accept the given alloy.
     * @param baseMold The base casting mold
     * @param alloy The alloy type
     * @return The recipe if found and accepts the alloy, empty otherwise
     */
    public @NotNull Optional<CastingMoldRecipe> findRecipeForAlloy(@NotNull BaseItem baseMold, @NotNull Alloy alloy) {
        return findRecipe(baseMold)
                .filter(recipe -> recipe.acceptsAlloy(alloy));
    }
    
    /**
     * Gets all registered casting mold recipes.
     * @return An unmodifiable list of all recipes
     */
    public @NotNull List<CastingMoldRecipe> getAllRecipes() {
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
        log.info("Cleared all casting mold recipes").submit();
    }
} 