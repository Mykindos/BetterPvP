package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for managing all recipes in the system.
 * Handles registration, lookup, and matching of recipes.
 */
@CustomLog
@Singleton
public class CraftingRecipeRegistry implements RecipeRegistry<CraftingRecipe> {

    private final RecipeResolver<CraftingRecipe> resolver;
    private final Map<NamespacedKey, CraftingRecipe> craftingRecipes = new HashMap<>();

    @Inject
    private CraftingRecipeRegistry(RecipeRegistries registries, MinecraftCraftingRecipeAdapter minecraftAdapter) {
        this.resolver = new RecipeResolver<>(this);
        minecraftAdapter.registerDefaults(craftingRecipes);
        registries.register(new NamespacedKey("betterpvp", "crafting"), this);
    }

    @Override
    public RecipeResolver<CraftingRecipe> getResolver() {
        return resolver;
    }

    /**
     * Registers a new recipe.
     * @param key The key to register the recipe under
     * @param craftingRecipe The recipe to register
     */
    public void registerRecipe(@NotNull NamespacedKey key, @NotNull CraftingRecipe craftingRecipe) {
        if (craftingRecipes.containsKey(key)) {
            log.warn("Recipe with key {} is already registered, overwriting", key).submit();
        }

        craftingRecipes.put(key, craftingRecipe);
        log.info("Registered recipe: {}", key).submit();
    }

    
    /**
     * Gets all registered recipes.
     * 
     * @return An unmodifiable set of all recipes
     */
    @NotNull
    public Set<CraftingRecipe> getRecipes() {
        return Set.copyOf(craftingRecipes.values());
    }

    /**
     * Finds the first recipe that matches the provided items.
     * 
     * @param items The items to match against
     * @param type The type of recipe to match (or null for any type)
     * @return The first matching recipe, or empty if none match
     */
    @NotNull
    public Optional<CraftingRecipe> findMatchingRecipe(@NotNull Map<Integer, ItemStack> items, @Nullable RecipeType type) {
        // First check custom recipes
        for (CraftingRecipe craftingRecipe : craftingRecipes.values()) {
            if (type != null && craftingRecipe.getType() != type) {
                continue;
            }
            
            if (craftingRecipe.matches(items)) {
                return Optional.of(craftingRecipe);
            }
        }
        
        return Optional.empty();
    }
}