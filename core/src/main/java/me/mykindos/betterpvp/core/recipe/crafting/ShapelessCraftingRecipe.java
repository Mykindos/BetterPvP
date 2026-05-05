package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A shapeless recipe that requires specific ingredients but not in any particular arrangement.
 */
@Getter
public class ShapelessCraftingRecipe implements CraftingRecipe {

    private final Supplier<ItemInstance> resultSupplier;
    private final Map<Integer, RecipeIngredient> ingredients;
    private final ItemFactory itemFactory;
    private final boolean needsBlueprint;

    @Nullable
    private NamespacedKey recipeKey;
    
    /**
     * Creates a new shapeless recipe with a single resultSupplier.
     * 
     * @param resultSupplier The resultSupplier of the recipe
     * @param ingredients The ingredients required (slot positions are ignored for matching)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapelessCraftingRecipe(@NotNull Supplier<ItemInstance> resultSupplier, @NotNull Map<Integer, RecipeIngredient> ingredients, @NotNull ItemFactory itemFactory, boolean needsBlueprint) {
        this.resultSupplier = resultSupplier;
        this.ingredients = new HashMap<>(ingredients);
        this.itemFactory = itemFactory;
        this.needsBlueprint = needsBlueprint;
    }

    public ShapelessCraftingRecipe(@NotNull BaseItem result, @NotNull Map<Integer, RecipeIngredient> ingredients, @NotNull ItemFactory itemFactory, boolean needsBlueprint) {
        this(() -> itemFactory.create(result), ingredients, itemFactory, needsBlueprint);
    }
    
    @Override
    public @NotNull ItemInstance getPrimaryResult() {
        return resultSupplier.get();
    }

    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return resultSupplier.get();
    }

    @Override
    public boolean needsBlueprint() {
        return needsBlueprint;
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Create a map of BaseItem to required amounts
        Map<BaseItem, Integer> requiredIngredients = new HashMap<>();
        for (RecipeIngredient ingredient : ingredients.values()) {
            requiredIngredients.merge(ingredient.getBaseItem(), ingredient.getAmount(), Integer::sum);
        }
        
        // Create a map of BaseItem to available amounts
        Multimap<BaseItem, Integer> availableIngredients = ArrayListMultimap.create();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            
            itemFactory.fromItemStack(stack).ifPresent(instance -> {
                BaseItem baseItem = instance.getBaseItem();
                availableIngredients.put(baseItem, stack.getAmount());
            });
        }
        
        // Check if all required ingredients are available in sufficient quantities
        outer:
        for (Map.Entry<BaseItem, Integer> entry : requiredIngredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int requiredAmount = entry.getValue();

            if (!availableIngredients.containsKey(baseItem)) {
                return false;
            }
            
            Collection<Integer> availableAmounts = availableIngredients.get(baseItem);
            for (int availableAmount : availableAmounts) {
                if (availableAmount >= requiredAmount) {
                    // remove entry
                    availableIngredients.remove(baseItem, availableAmount);
                    continue outer;
                }
            }

            return false;
        }
        
        return availableIngredients.isEmpty();
    }

    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.SHAPELESS_CRAFTING;
    }

    /** Called by {@link CraftingRecipeRegistry} to store the key after registration. */
    public void setRecipeKey(@NotNull NamespacedKey key) {
        this.recipeKey = key;
    }

    /** Delegates to {@link ItemAccessService#isAllowed} with {@link AccessScope#CRAFT}. */
    @Override
    public boolean canCraft(@Nullable Player player) {
        if (player == null || recipeKey == null) return true;
        try {
            ItemAccessService service = JavaPlugin.getPlugin(Core.class)
                    .getInjector().getInstance(ItemAccessService.class);
            Key key = Key.key(recipeKey.namespace(), recipeKey.getKey());
            return service.isAllowed(player, resultSupplier.get().getBaseItem(), key, AccessScope.CRAFT);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();

        // Create a map of BaseItem to required amounts (only for items that should be consumed)
        Map<BaseItem, Integer> requiredIngredients = new HashMap<>();
        for (RecipeIngredient ingredient : getIngredients().values()) {
            if (ingredient.isConsumeOnCraft()) {
                requiredIngredients.merge(ingredient.getBaseItem(), ingredient.getAmount(), Integer::sum);
            }
        }

        // Consume items from the matrix
        for (Map.Entry<BaseItem, Integer> entry : requiredIngredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int amountToConsume = entry.getValue();

            // Find matching items in the matrix and consume them
            for (Map.Entry<Integer, ItemInstance> matrixEntry : new HashMap<>(ingredients).entrySet()) {
                if (amountToConsume <= 0) {
                    break;
                }

                int slot = matrixEntry.getKey();
                ItemInstance instance = matrixEntry.getValue();

                if (instance == null) {
                    continue;
                }

                if (!instance.getBaseItem().equals(baseItem)) {
                    continue;
                }

                ItemStack stack = instance.createItemStack();
                // Consume from this stack
                int amountFromThisStack = Math.min(stack.getAmount(), amountToConsume);
                amountToConsume -= amountFromThisStack;

                int newAmount = stack.getAmount() - amountFromThisStack;
                if (newAmount <= 0) {
                    ingredients.remove(slot);
                } else {
                    stack.setAmount(newAmount);
                    final ItemInstance result = itemFactory.fromItemStack(stack).orElseThrow();
                    ingredients.put(slot, result);
                }

                consumedSlots.add(slot);
            }
        }
        
        return consumedSlots;
    }
} 