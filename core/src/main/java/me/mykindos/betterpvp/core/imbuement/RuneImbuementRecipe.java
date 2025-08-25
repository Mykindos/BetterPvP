package me.mykindos.betterpvp.core.imbuement;

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
import java.util.Set;

/**
 * Specialized imbuement recipe for applying runes to items.
 * Dynamically matches any rune + compatible item combination.
 */
@Getter
public class RuneImbuementRecipe extends ImbuementRecipe {
    
    /**
     * Creates a new rune imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    public RuneImbuementRecipe(@NotNull ItemFactory itemFactory) {
        super(itemFactory);
    }
    
    @Override
    public @NotNull ImbuementRecipeResult getPrimaryResult() {
        // This will be dynamically determined based on the target item
        throw new UnsupportedOperationException("Rune recipes determine result dynamically");
    }
    
    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        // This should be called with context from the matching process
        throw new UnsupportedOperationException("Use createPrimaryResult(List<ItemInstance>) for rune recipes");
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
        
        return applyRuneToItem(runeData.targetItem, runeData.rune);
    }
    
    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Convert to ItemInstance list
        List<ItemInstance> itemInstances = new ArrayList<>();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            
            Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(stack);
            instanceOpt.ifPresent(itemInstances::add);
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
        Rune rune = null;
        
        // Identify rune and target items
        for (ItemInstance item : items) {
            ItemStack stack = item.createItemStack();
            
            // Check if this is a rune item
            if (item.getBaseItem() instanceof RuneItem runeItem) {
                if (runeItemInstance != null) {
                    return null; // Multiple rune items not allowed
                }
                if (stack.getAmount() != 1) {
                    return null; // Rune stack must be exactly 1
                }
                runeItemInstance = item;
                rune = runeItem.getRune();
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
        if (runeItemInstance == null || targetItemInstance == null || rune == null) {
            return null;
        }
        
        // Check if rune can be applied to target item
        if (!rune.canApply(targetItemInstance)) {
            return null;
        }
        
        // Check if target item can accept more runes
        Optional<RuneContainerComponent> containerOpt = targetItemInstance.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty() || !containerOpt.get().hasAvailableSockets()) {
            return null;
        }

        // Check if rune is already applied
        if (containerOpt.get().hasRune(rune)) {
            return null; // Rune already applied to target item
        }
        
        return new RuneData(rune, runeItemInstance, targetItemInstance);
    }
    
    /**
     * Applies a rune to an item and returns the modified item instance.
     */
    private ItemInstance applyRuneToItem(@NotNull ItemInstance targetItem, @NotNull Rune rune) {
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
        newRunes.add(rune);
        RuneContainerComponent newContainer = new RuneContainerComponent(existing.getSockets(), newRunes);
        
        // Apply the updated container to the target item
        return targetItem.withComponent(newContainer);
    }
    
    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        // Rune recipes don't have fixed ingredients
        return new HashMap<>();
    }
    
    @Override
    public @NotNull Set<BaseItem> getIngredientTypes() {
        // Rune recipes don't have fixed ingredient types
        return Set.of();
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