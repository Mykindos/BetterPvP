package me.mykindos.betterpvp.core.block.impl.imbuement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.TickHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Main coordinator class for ImbuementPedestal functionality.
 * Delegates responsibilities to specialized components.
 */
@RequiredArgsConstructor
@Getter
public class ImbuementPedestalData implements RemovalHandler, LoadHandler, TickHandler {

    private final ItemFactory itemFactory;
    private final ImbuementRecipeRegistry imbuementRecipeRegistry;

    // Component managers
    private final ImbuementPedestalItemManager itemManager;
    private final ImbuementPedestalDisplayManager displayManager;
    private final ImbuementPedestalRecipeExecutor recipeExecutor;

    // Constructor for dependency injection
    public ImbuementPedestalData(@NotNull ItemFactory itemFactory,
                                 @NotNull ImbuementRecipeRegistry imbuementRecipeRegistry) {
        this.itemFactory = itemFactory;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;

        // Initialize component managers
        this.itemManager = new ImbuementPedestalItemManager(imbuementRecipeRegistry);
        this.displayManager = new ImbuementPedestalDisplayManager();
        this.recipeExecutor = new ImbuementPedestalRecipeExecutor(itemFactory, displayManager);
    }

    /**
     * Adds an item to the pedestal.
     *
     * @param item The item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(@NotNull ItemInstance item) {
        if (itemManager.isFull() || recipeExecutor.isExecutingRecipe()) {
            return false;
        }

        boolean success = itemManager.addItem(item);
        if (success) {
            // Create flying display entity for the new item
            displayManager.createFlyingItemDisplay(item);
            updateRecipeReadyDisplay();
        }

        return success;
    }

    /**
     * Removes the last item from the pedestal.
     *
     * @return The removed item, or null if the pedestal is empty
     */
    public ItemInstance removeLastItem() {
        if (itemManager.getItems().isEmpty() || recipeExecutor.isExecutingRecipe()) {
            return null;
        }

        ItemInstance removedItem = itemManager.removeLastItem();
        if (removedItem != null) {
            displayManager.removeLastFlyingItem();
            updateRecipeReadyDisplay();
        }

        return removedItem;
    }

    /**
     * Executes the current recipe if available.
     *
     * @param player The player executing the recipe
     * @return true if recipe execution started, false otherwise
     */
    public boolean executeRecipe(@NotNull Player player) {
        if (itemManager.getCurrentRecipe() == null || recipeExecutor.isExecutingRecipe() || !itemManager.hasItems()) {
            return false;
        }

        boolean success = recipeExecutor.executeRecipe(itemManager.getCurrentRecipe(), player);
        if (success) {
            updateRecipeReadyDisplay();
        } else {
            itemManager.clearAllItems();
        }

        return success;
    }

    /**
     * Updates the recipe ready text display.
     */
    private void updateRecipeReadyDisplay() {
        displayManager.updateRecipeReadyDisplay(
                itemManager.hasRecipe(),
                recipeExecutor.isExecutingRecipe(),
                itemManager.hasItems()
        );
    }

    /**
     * Gets the pedestal location.
     */
    public Location getPedestalLocation() {
        return displayManager.getPedestalLocation();
    }

    /**
     * Sets the pedestal location for display entities.
     */
    public void setPedestalLocation(@NotNull Location location) {
        displayManager.setPedestalLocation(location);
    }

    /**
     * Refreshes all display entities by recreating them.
     */
    public void refreshDisplayEntities() {
        displayManager.refreshDisplayEntities(itemManager.getItems());
        updateRecipeReadyDisplay();
    }

    // Utility methods delegating to item manager
    public int getItemCount() {
        return itemManager.getItemCount();
    }

    public boolean isFull() {
        return itemManager.isFull();
    }

    public boolean hasItems() {
        return itemManager.hasItems();
    }

    @Override
    public void onTick(@NotNull SmartBlockInstance instance) {
        if (recipeExecutor.isExecutingRecipe()) {
            // Update recipe execution and check for completion
            if (recipeExecutor.updateRecipeExecution()) {
                // Recipe execution completed
                recipeExecutor.completeRecipe(itemManager.getCurrentRecipe(), itemManager.getItems());
                itemManager.clearAllItems();
                updateRecipeReadyDisplay();
            }
        } else {
            // Update normal flying item animations
            displayManager.updateFlyingItems();
        }
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        // Clean up display entities
        displayManager.cleanup();

        // Handle item removal through item manager
        itemManager.onRemoval(instance, cause);

        // Reset recipe executor
        recipeExecutor.reset();
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        // Clean up display entities
        displayManager.cleanup();

        // Handle item unload through item manager
        itemManager.onUnload(instance);

        // Reset recipe executor
        recipeExecutor.reset();
    }

    @Override
    public void onLoad(@NotNull SmartBlockInstance instance) {
        setPedestalLocation(instance.getLocation());

        // Reinitialize display entities if needed
        displayManager.refreshDisplayEntities(itemManager.getItems());
        updateRecipeReadyDisplay();
    }
}