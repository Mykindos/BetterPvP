package me.mykindos.betterpvp.core.recipe.crafting;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A shapeless recipe that requires specific ingredients but not in any particular arrangement.
 */
@Getter
public class ShapelessCraftingRecipe implements CraftingRecipe {

    private final BaseItem result;
    private final Consumer<ItemInstance> resultCustomizer;
    private final Map<Integer, RecipeIngredient> ingredients;
    private final ItemFactory itemFactory;
    private final boolean needsBlueprint;

    @Nullable
    private NamespacedKey recipeKey;

    /**
     * Creates a new shapeless recipe.
     * @param result The base item produced
     * @param resultCustomizer Side-effect-free customizer applied to both preview and live results
     * @param ingredients The ingredients required (slot positions are ignored for matching)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapelessCraftingRecipe(@NotNull BaseItem result,
                                   @NotNull Consumer<ItemInstance> resultCustomizer,
                                   @NotNull Map<Integer, RecipeIngredient> ingredients,
                                   @NotNull ItemFactory itemFactory,
                                   boolean needsBlueprint) {
        this.result = result;
        this.resultCustomizer = resultCustomizer;
        this.ingredients = new HashMap<>(ingredients);
        this.itemFactory = itemFactory;
        this.needsBlueprint = needsBlueprint;
    }

    public ShapelessCraftingRecipe(@NotNull BaseItem result,
                                   @NotNull Map<Integer, RecipeIngredient> ingredients,
                                   @NotNull ItemFactory itemFactory,
                                   boolean needsBlueprint) {
        this(result, instance -> {}, ingredients, itemFactory, needsBlueprint);
    }

    @Override
    public @NotNull ItemInstance previewResult() {
        ItemInstance instance = itemFactory.createPreview(result);
        resultCustomizer.accept(instance);
        return instance;
    }

    @Override
    public @NotNull ItemInstance createResult() {
        ItemInstance instance = itemFactory.create(result);
        resultCustomizer.accept(instance);
        return instance;
    }

    @Override
    public boolean needsBlueprint() {
        return needsBlueprint;
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        List<Integer> nonAirSlots = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack != null && !stack.getType().isAir()) {
                nonAirSlots.add(entry.getKey());
            }
        }

        if (nonAirSlots.size() != ingredients.size()) {
            return false;
        }

        // Greedy per-ingredient slot claim: each ingredient must find an unclaimed grid slot it accepts.
        // Order ingredients by acceptance breadth (most-restrictive first) so a narrow ingredient
        // doesn't lose its only viable slot to a broader one that had alternatives.
        List<RecipeIngredient> orderedIngredients = new ArrayList<>(ingredients.values());
        orderedIngredients.sort(Comparator.comparingInt(a -> a.getBaseItems().size()));

        Set<Integer> claimed = new HashSet<>();
        for (RecipeIngredient ingredient : orderedIngredients) {
            boolean matched = false;
            for (int slot : nonAirSlots) {
                if (claimed.contains(slot)) continue;
                if (ingredient.matches(items.get(slot), itemFactory)) {
                    claimed.add(slot);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        return true;
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
            return service.isAllowed(player, result, key, AccessScope.CRAFT);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();

        // Order ingredients narrowest-first so we don't strand a single-item ingredient
        // by greedily claiming its only viable slot for a broader one.
        List<RecipeIngredient> orderedIngredients = new ArrayList<>(getIngredients().values());
        orderedIngredients.sort((a, b) -> Integer.compare(a.getBaseItems().size(), b.getBaseItems().size()));

        Set<Integer> claimed = new HashSet<>();
        for (RecipeIngredient ingredient : orderedIngredients) {
            int amountToConsume = ingredient.getAmount();

            for (Map.Entry<Integer, ItemInstance> matrixEntry : ingredients.entrySet()) {
                if (amountToConsume <= 0) {
                    break;
                }

                int slot = matrixEntry.getKey();
                if (claimed.contains(slot)) {
                    continue;
                }

                ItemInstance instance = matrixEntry.getValue();
                if (instance == null) {
                    continue;
                }
                if (!ingredient.accepts(instance.getBaseItem())) {
                    continue;
                }

                ItemStack stack = instance.createItemStack();
                if (stack.getAmount() < amountToConsume) {
                    continue;
                }

                claimed.add(slot);

                if (!ingredient.isConsumeOnCraft()) {
                    amountToConsume = 0;
                    break;
                }

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
                break;
            }
        }

        return consumedSlots;
    }
}
