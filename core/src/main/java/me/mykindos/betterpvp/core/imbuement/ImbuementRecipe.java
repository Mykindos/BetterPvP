package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for imbuement recipes.
 * Provides common functionality while allowing specialized implementations for different recipe types.
 */
@Getter
public abstract class ImbuementRecipe implements Recipe<ImbuementRecipeResult, ItemInstance> {
    
    protected final @NotNull ItemFactory itemFactory;
    
    /**
     * Creates a new imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    protected ImbuementRecipe(@NotNull ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }
    
    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.IMBUEMENT;
    }
    
    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();
        
        // For imbuement recipes, we consume ALL ingredients exactly
        for (Map.Entry<Integer, ItemInstance> entry : new HashMap<>(ingredients).entrySet()) {
            if (entry.getValue() != null) {
                ingredients.remove(entry.getKey());
                consumedSlots.add(entry.getKey());
            }
        }
        
        return consumedSlots;
    }
} 