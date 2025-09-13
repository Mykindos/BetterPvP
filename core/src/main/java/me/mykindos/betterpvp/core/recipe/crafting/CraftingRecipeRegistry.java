package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftRecipeAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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

    private final ItemRegistry itemRegistry;
    private final MinecraftRecipeAdapter minecraftAdapter;
    private final RecipeResolver<CraftingRecipe> resolver;
    private final Set<CraftingRecipe> craftingRecipes = new HashSet<>();

    @Inject
    private CraftingRecipeRegistry(RecipeRegistries registries, ItemRegistry itemRegistry, MinecraftRecipeAdapter minecraftAdapter) {
        this.itemRegistry = itemRegistry;
        this.minecraftAdapter = minecraftAdapter;
        this.resolver = new RecipeResolver<>(this);
        registries.register(this);
    }

    @Override
    public RecipeResolver<CraftingRecipe> getResolver() {
        return resolver;
    }

    /**
     * Registers a new recipe.
     *
     * @param craftingRecipe The recipe to register
     */
    public void registerRecipe(@NotNull CraftingRecipe craftingRecipe) {
        // Add to result lookup multimap
        BaseItem resultItem = craftingRecipe.getPrimaryResult();
        NamespacedKey itemKey = itemRegistry.getKey(resultItem);
        craftingRecipes.add(craftingRecipe);
        log.info("Registered recipe for item: {}", itemKey).submit();
    }
    
    
    /**
     * Gets all registered recipes.
     * 
     * @return An unmodifiable set of all recipes
     */
    @NotNull
    public Set<CraftingRecipe> getRecipes() {
        return Collections.unmodifiableSet(craftingRecipes);
    }
    
    /**
     * Gets all recipes of a specific type.
     * 
     * @param type The recipe type to filter by
     * @return A list of recipes of the specified type
     */
    @NotNull
    public List<CraftingRecipe> getRecipesByType(@NotNull RecipeType type) {
        List<CraftingRecipe> result = new ArrayList<>();
        for (CraftingRecipe craftingRecipe : craftingRecipes) {
            if (craftingRecipe.getType() == type) {
                result.add(craftingRecipe);
            }
        }
        return result;
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
        for (CraftingRecipe craftingRecipe : craftingRecipes) {
            if (type != null && craftingRecipe.getType() != type) {
                continue;
            }
            
            if (craftingRecipe.matches(items)) {
                return Optional.of(craftingRecipe);
            }
        }
        
        // If no custom recipe matches and Minecraft recipes are enabled, check Minecraft recipes
        if (minecraftAdapter.isEnabled() && (type == null || type == RecipeType.SHAPED_CRAFTING || type == RecipeType.SHAPELESS_CRAFTING)) {
            Optional<org.bukkit.inventory.Recipe> minecraftRecipe = minecraftAdapter.findMatchingRecipe(items);
            if (minecraftRecipe.isPresent()) {
                // Convert Minecraft recipe to our recipe format
                return Optional.of(minecraftAdapter.convertToCustomRecipe(minecraftRecipe.get()));
            }
        }
        
        return Optional.empty();
    }
    
} 