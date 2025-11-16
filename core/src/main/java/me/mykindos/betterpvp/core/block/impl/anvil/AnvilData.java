package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Main coordinator class for Anvil functionality.
 * Delegates responsibilities to specialized components.
 */
@RequiredArgsConstructor
@Getter
public class AnvilData implements RemovalHandler, LoadHandler {

    private final ItemFactory itemFactory;
    private final AnvilRecipeRegistry anvilRecipeRegistry;

    // Component managers
    private final AnvilItemManager itemManager;
    private final AnvilDisplayManager displayManager;
    private final AnvilHammerExecutor hammerExecutor;

    // Constructor for dependency injection
    public AnvilData(@NotNull ItemFactory itemFactory,
                     @NotNull AnvilRecipeRegistry anvilRecipeRegistry) {
        this.itemFactory = itemFactory;
        this.anvilRecipeRegistry = anvilRecipeRegistry;

        // Initialize component managers
        this.itemManager = new AnvilItemManager(anvilRecipeRegistry);
        this.displayManager = new AnvilDisplayManager();
        this.hammerExecutor = new AnvilHammerExecutor(itemFactory);
    }

    /**
     * Executes a hammer swing, incrementing the counter and checking for recipe completion.
     *
     * @param player   The player swinging the hammer
     * @param location The location to play effects at
     */
    public void executeHammerSwing(@NotNull Player player, @NotNull Location location) {
        if (!hammerExecutor.executeHammerSwing(player, location)) {
            return; // Cooldown not finished
        }

        // Update progress display
        updateHammerProgressDisplay();

        // Check if we have a current recipe and enough swings
        if (itemManager.getCurrentRecipe() != null &&
                hammerExecutor.hasEnoughSwings(itemManager.getCurrentRecipe())) {
            executeRecipe(player, location);
        }
    }

    /**
     * Adds an item to the anvil.
     *
     * @param item The item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(@NotNull ItemInstance item) {
        if (itemManager.isFull()) {
            return false;
        }

        boolean success = itemManager.addItem(item);
        if (success) {
            // Create display entity for the new item
            displayManager.createDisplayEntityForItem(item, itemManager.getItemCount() - 1);

            // Reset hammer swings when items change
            hammerExecutor.reset();
            updateHammerProgressDisplay();
        }

        return success;
    }

    /**
     * Removes the last item from the anvil.
     *
     * @return The removed item, or null if the anvil is empty
     */
    public ItemInstance removeLastItem() {
        ItemInstance removedItem = itemManager.removeLastItem();
        if (removedItem != null) {
            // Remove display entity for the last item
            displayManager.removeLastDisplayEntity();

            // Reset hammer swings when items change
            hammerExecutor.reset();
            updateHammerProgressDisplay();
        }

        return removedItem;
    }

    /**
     * Executes the current recipe, consuming ingredients and producing results.
     *
     * @param player   The player who completed the recipe
     * @param location The location to play effects at
     */
    private void executeRecipe(@NotNull Player player, @NotNull Location location) {
        if (itemManager.getCurrentRecipe() == null) {
            return;
        }

        // Get items as map for recipe execution
        Map<Integer, ItemInstance> itemsMap = itemManager.getItemsAsMap();

        // Execute recipe and get remaining items
        List<ItemInstance> remainingItems = hammerExecutor.executeRecipe(
                itemManager.getCurrentRecipe(), itemsMap, location
        );

        // Update anvil items with remaining items
        itemManager.updateItemsAfterRecipe(remainingItems);

        // Update display entities to match remaining items
        displayManager.updateDisplayEntitiesAfterRecipe(remainingItems);

        // Update progress display
        updateHammerProgressDisplay();
    }

    /**
     * Updates the hammer progress display.
     */
    private void updateHammerProgressDisplay() {
        if (itemManager.getCurrentRecipe() != null) {
            displayManager.updateHammerProgressDisplay(
                    itemManager.hasRecipe(),
                    itemManager.hasItems(),
                    hammerExecutor.getHammerSwings(),
                    itemManager.getCurrentRecipe().getRequiredHammerSwings()
            );
        } else {
            displayManager.updateHammerProgressDisplay(false, itemManager.hasItems(), 0, 0);
        }
    }

    /**
     * Gets the anvil location.
     *
     * @return The anvil location, or null if not set
     */
    public Location getAnvilLocation() {
        return displayManager.getAnvilLocation();
    }

    /**
     * Sets the anvil location for display entity management.
     *
     * @param location The location of the anvil
     */
    public void setAnvilLocation(@NotNull Location location) {
        displayManager.setAnvilLocation(location);
    }

    /**
     * Refreshes all display entities to match current items.
     */
    public void refreshDisplayEntities() {
        displayManager.refreshDisplayEntities(itemManager.getItems());
        updateHammerProgressDisplay();
    }

    // Utility methods delegating to components
    public boolean canSwing() {
        return hammerExecutor.canSwing();
    }

    public int getItemCount() {
        return itemManager.getItemCount();
    }

    public boolean isFull() {
        return itemManager.isFull();
    }

    public boolean hasItems() {
        return itemManager.hasItems();
    }

    public float getProgress() {
        if (itemManager.getCurrentRecipe() == null) {
            return 0.0f;
        }
        return hammerExecutor.getProgress(itemManager.getCurrentRecipe());
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        // Clean up display entities
        displayManager.cleanup();

        // Handle item removal through item manager
        itemManager.onRemoval(instance, cause);
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        // Clean up display entities
        displayManager.cleanup();

        // Handle item unload through item manager
        itemManager.onUnload(instance);
    }

    @Override
    public void onLoad(@NotNull SmartBlockInstance instance) {
        setAnvilLocation(instance.getLocation());

        // Reinitialize display entities if needed
        refreshDisplayEntities();
    }
}