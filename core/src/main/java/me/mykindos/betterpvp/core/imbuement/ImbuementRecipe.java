package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for imbuement recipes.
 * Provides common functionality while allowing specialized implementations for different recipe types.
 */
@Getter
public abstract class ImbuementRecipe implements Recipe<ImbuementRecipeResult, ItemInstance> {
    
    protected final @NotNull ItemFactory itemFactory;

    @Nullable
    private NamespacedKey recipeKey;

    /**
     * Creates a new imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    protected ImbuementRecipe(@NotNull ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /** Called by {@link ImbuementRecipeRegistry} to store the key after registration. */
    public void setRecipeKey(@NotNull NamespacedKey key) {
        this.recipeKey = key;
    }

    @Override
    public boolean canCraft(@Nullable Player player) {
        if (player == null || recipeKey == null) return true;
        try {
            ItemAccessService service = JavaPlugin.getPlugin(Core.class)
                    .getInjector().getInstance(ItemAccessService.class);
            Key key = Key.key(recipeKey.namespace(), recipeKey.getKey());
            BaseItem resultItem = getPrimaryResult().getPrimaryResult();
            return service.isAllowed(player, resultItem, key, AccessScope.CRAFT);
        } catch (Exception e) {
            return true;
        }
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