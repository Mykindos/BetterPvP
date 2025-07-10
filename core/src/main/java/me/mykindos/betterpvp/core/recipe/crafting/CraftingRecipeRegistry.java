package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeType;
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
public class CraftingRecipeRegistry {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final MinecraftRecipeAdapter minecraftAdapter;
    private final Set<CraftingRecipe> craftingRecipes = new HashSet<>();
    private final Multimap<NamespacedKey, CraftingRecipe> recipesByResult = MultimapBuilder.hashKeys().hashSetValues().build();
    
    @Inject
    private CraftingRecipeRegistry(ItemFactory itemFactory, ItemRegistry itemRegistry, MinecraftRecipeAdapter minecraftAdapter) {
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.minecraftAdapter = minecraftAdapter;
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
        if (itemKey != null) {
            craftingRecipes.add(craftingRecipe);
            recipesByResult.put(itemKey, craftingRecipe);
            log.info("Registered recipe for item: {}", itemKey).submit();
        } else {
            log.warn("Tried registering recipe for unregistered item: {}", resultItem.getClass().getName()).submit();
        }
    }
    
    /**
     * Gets all recipes that produce a specific base item.
     * 
     * @param baseItem The base item to look up
     * @return An unmodifiable list of recipes that produce the item
     */
    @NotNull
    public List<CraftingRecipe> getRecipesForResult(@NotNull BaseItem baseItem) {
        NamespacedKey itemKey = itemRegistry.getKey(baseItem);
        if (itemKey == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(recipesByResult.get(itemKey));
    }
    
    /**
     * Gets all recipes that produce a specific base item by its key.
     * 
     * @param itemKey The namespaced key of the base item
     * @return An unmodifiable list of recipes that produce the item
     */
    @NotNull
    public List<CraftingRecipe> getRecipesForResult(@NotNull NamespacedKey itemKey) {
        return ImmutableList.copyOf(recipesByResult.get(itemKey));
    }
    
    /**
     * Gets all registered recipes.
     * 
     * @return An unmodifiable set of all recipes
     */
    @NotNull
    public Set<CraftingRecipe> getAllRecipes() {
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