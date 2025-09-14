package me.mykindos.betterpvp.core.imbuement;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Specialized imbuement recipe for applying runes to items.
 * Dynamically matches any rune + compatible item combination.
 */
@Getter
public class RuneImbuementRecipe extends ImbuementRecipe {

    private final BaseItem baseItem;
    private final RuneItem runeItem;

    /**
     * Creates a new rune imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    public RuneImbuementRecipe(@NotNull ItemFactory itemFactory, BaseItem baseItem, RuneItem rune) {
        super(itemFactory);
        final Optional<RuneContainerComponent> containerOpt = baseItem.getComponent(RuneContainerComponent.class);
        Preconditions.checkArgument(containerOpt.isPresent(), "Base item must have a rune container component");
        final RuneContainerComponent container = containerOpt.get();
        Preconditions.checkArgument(rune.getRune().canApply(baseItem), "Rune cannot be applied to base item");
        this.baseItem = baseItem;
        this.runeItem = rune;
    }
    
    @Override
    public @NotNull ImbuementRecipeResult getPrimaryResult() {
        return new ImbuementRecipeResult(baseItem);
    }
    
    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return createPrimaryResult(List.of(itemFactory.create(baseItem), itemFactory.create(runeItem)));
    }
    
    /**
     * Creates the primary result for a rune recipe with the given items.
     * @param items The items used in the recipe (should contain 1 rune + 1 target)
     * @return The target item with the rune applied
     */
    public @NotNull ItemInstance createPrimaryResult(@NotNull List<ItemInstance> items) {
        RuneData runeData = extractRuneData(items);
        if (runeData == null) {
            throw new IllegalArgumentException("Invalid rune recipe items");
        }
        
        return applyRuneToItem(runeData.targetItem);
    }
    
    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Convert to ItemInstance list
        List<ItemInstance> itemInstances = new ArrayList<>();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }

            itemFactory.fromItemStack(stack).ifPresent(itemInstances::add);
        }

        return isValidRuneRecipe(itemInstances);
    }
    
    /**
     * Checks if the given items form a valid rune recipe.
     * A rune recipe consists of exactly one rune item and one target item (amount = 1 each).
     */
    private boolean isValidRuneRecipe(@NotNull List<ItemInstance> items) {
        if (items.size() != 2) {
            return false;
        }
        
        RuneData runeData = extractRuneData(items);
        return runeData != null;
    }
    
    /**
     * Extracts rune and target item data from the given items.
     * @param items The items to analyze
     * @return RuneData if valid, null otherwise
     */
    private RuneData extractRuneData(@NotNull List<ItemInstance> items) {
        ItemInstance runeItemInstance = null;
        ItemInstance targetItemInstance = null;
        
        // Identify rune and target items
        for (ItemInstance item : items) {
            if (item == null) continue;
            ItemStack stack = item.createItemStack();
            
            // Check if this is a rune item
            if (item.getBaseItem() == runeItem) {
                if (runeItemInstance != null) {
                    return null; // Multiple rune items not allowed
                }
                if (stack.getAmount() != 1) {
                    return null; // Rune stack must be exactly 1
                }
                runeItemInstance = item;
            } else {
                if (targetItemInstance != null) {
                    return null; // Multiple target items not allowed
                }
                if (stack.getAmount() != 1) {
                    return null; // Target stack must be exactly 1
                }
                targetItemInstance = item;
            }
        }
        
        // Both rune and target must be present
        if (runeItemInstance == null || targetItemInstance == null) {
            return null;
        }
        
        // Check if rune can be applied to target item
        if (!runeItem.getRune().canApply(targetItemInstance)) {
            return null;
        }
        
        // Check if target item can accept more runes
        Optional<RuneContainerComponent> containerOpt = targetItemInstance.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty() || !containerOpt.get().hasAvailableSockets()) {
            return null;
        }

        // Check if rune is already applied
        if (containerOpt.get().hasRune(runeItem.getRune())) {
            return null; // Rune already applied to target item
        }
        
        return new RuneData(runeItem.getRune(), runeItemInstance, targetItemInstance);
    }
    
    /**
     * Applies a rune to an item and returns the modified item instance.
     */
    private ItemInstance applyRuneToItem(@NotNull ItemInstance targetItem) {
        // Get existing rune container - we should only work with existing containers
        Optional<RuneContainerComponent> containerOpt = targetItem.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty()) {
            throw new IllegalArgumentException("Target item does not have a rune container component");
        }
        
        RuneContainerComponent existing = containerOpt.get();
        
        // Verify the container has available slots
        if (!existing.hasAvailableSockets()) {
            throw new IllegalArgumentException("Target item's rune container is full");
        }
        
        // Create a new container with the rune added
        List<Rune> newRunes = new ArrayList<>(existing.getRunes());
        newRunes.add(runeItem.getRune());
        RuneContainerComponent newContainer = new RuneContainerComponent(existing.getSockets(), newRunes);
        
        // Apply the updated container to the target item
        return targetItem.withComponent(newContainer);
    }
    
    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        return Map.of(
                0, new RecipeIngredient(runeItem, 1),
                1, new RecipeIngredient(baseItem, 1)
        );
    }
    
    /**
     * Data class to hold rune recipe components.
     */
    private static class RuneData {
        final Rune rune;
        final ItemInstance runeItem;
        final ItemInstance targetItem;
        
        RuneData(Rune rune, ItemInstance runeItem, ItemInstance targetItem) {
            this.rune = rune;
            this.runeItem = runeItem;
            this.targetItem = targetItem;
        }
    }
} 