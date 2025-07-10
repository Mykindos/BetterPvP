package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeResult;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.UnloadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import javax.swing.plaf.basic.ComboPopup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Setter
public class AnvilData implements RemovalHandler, UnloadHandler {

    private final ItemFactory itemFactory;
    private final AnvilRecipeRegistry anvilRecipeRegistry;
    private StorageBlockData anvilItems = new StorageBlockData(10); // 10 slots for anvil items
    private int hammerSwings = 0;
    private long lastSwingTime = 0L; // System.currentTimeMillis()
    private AnvilRecipe currentRecipe = null; // Current matched recipe
    private final List<List<ItemDisplay>> itemDisplayEntities = new ArrayList<>(); // Display entities for each item
    private Location anvilLocation = null; // Location of the anvil for display entities
    private TextDisplay hammerProgressDisplay = null; // Text display showing hammer swing progress
    
    private static final long SWING_COOLDOWN_MS = 1000L; // 1 second universal cooldown
    private static final double DISPLAY_HEIGHT_OFFSET = 1.005; // Height above anvil to show items
    private static final double STACK_HEIGHT_OFFSET = 0.03; // Height between stacked items
    private static final double TEXT_DISPLAY_HEIGHT_OFFSET = 1.5; // Height above anvil for text display

    /**
     * Checks if a player can swing the hammer (cooldown check).
     * @return true if enough time has passed since the last swing
     */
    public boolean canSwing() {
        return System.currentTimeMillis() - lastSwingTime >= SWING_COOLDOWN_MS;
    }

    /**
     * Executes a hammer swing, incrementing the counter and checking for recipe completion.
     * @param player The player swinging the hammer
     * @param location The location to play effects at
     */
    public void executeHammerSwing(@NotNull Player player, @NotNull Location location) {
        if (!canSwing()) {
            return; // Cooldown not finished
        }

        lastSwingTime = System.currentTimeMillis();
        hammerSwings++;

        // Play hammer swing effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 0.7f, 0.4f).play(location);
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.2f + (float) Math.random() * 0.45f, 0.4f).play(location);
        player.swingMainHand();

        // Update progress display
        updateHammerProgressDisplay();
        
        // Check if we have a current recipe and enough swings
        if (currentRecipe != null && hammerSwings >= currentRecipe.getRequiredHammerSwings()) {
            executeRecipe(player, location);
        }
    }

    /**
     * Adds an item to the anvil.
     * @param item The item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(@NotNull ItemInstance item) {
        List<ItemInstance> currentItems = anvilItems.getContent();
        if (isFull()) {
            return false; // Anvil is full
        }

        // Add the item
        currentItems.add(item);
        anvilItems.setContent(currentItems);
        
        // Create display entity for the new item
        if (anvilLocation != null) {
            createDisplayEntityForItem(item, currentItems.size() - 1);
        }
        
        // Reset hammer swings and check for new recipe
        resetAndCheckRecipe();
        
        // Update progress display
        updateHammerProgressDisplay();
        return true;
    }

    /**
     * Removes the last item from the anvil.
     * @return The removed item, or null if the anvil is empty
     */
    public ItemInstance removeLastItem() {
        List<ItemInstance> currentItems = anvilItems.getContent();
        if (currentItems.isEmpty()) {
            return null;
        }

        // Remove the last item
        ItemInstance removedItem = currentItems.removeLast();
        anvilItems.setContent(currentItems);
        
        // Remove display entity for the last item
        removeLastDisplayEntity();
        
        // Reset hammer swings and check for new recipe
        resetAndCheckRecipe();
        
        // Update progress display
        updateHammerProgressDisplay();
        return removedItem;
    }

    /**
     * Resets hammer swings and checks for a matching recipe.
     */
    private void resetAndCheckRecipe() {
        hammerSwings = 0;
        currentRecipe = null;

        // Convert items to the format expected by recipe matching
        Map<Integer, ItemStack> itemMap = new HashMap<>();
        List<ItemInstance> items = anvilItems.getContent();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                itemMap.put(i, item.createItemStack());
            }
        }

        // Find a matching recipe
        Optional<AnvilRecipe> recipeOpt = anvilRecipeRegistry.findRecipe(itemMap);
        recipeOpt.ifPresent(anvilRecipe -> {
            currentRecipe = anvilRecipe;
        });
        
        // Update progress display
        updateHammerProgressDisplay();
    }

    /**
     * Executes the current recipe, consuming ingredients and producing results.
     * @param player The player who completed the recipe
     * @param location The location to play effects at
     */
    private void executeRecipe(@NotNull Player player, @NotNull Location location) {
        if (currentRecipe == null) {
            return;
        }

        // Convert items to ItemInstance map for recipe execution
        Map<Integer, ItemInstance> itemInstanceMap = new HashMap<>();
        List<ItemInstance> items = anvilItems.getContent();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                itemInstanceMap.put(i, item);
            }
        }

        // Consume ingredients
        currentRecipe.consumeIngredients(itemInstanceMap, itemFactory);

        // Update anvil items after consumption
        List<ItemInstance> newItems = itemInstanceMap.values().stream()
                .filter(Objects::nonNull)
                .toList();
        anvilItems.setContent(newItems);
        
        // Update display entities to match remaining items (preserving unchanged items)
        updateDisplayEntitiesAfterRecipe();

        // Get the recipe result
        AnvilRecipeResult result = currentRecipe.getResult();

        // Drop the primary result
        ItemStack primaryResult = itemFactory.create(result.getPrimaryResult()).createItemStack();
        location.getWorld().dropItemNaturally(location, primaryResult);

        // Drop secondary results
        for (var secondaryResult : result.getSecondaryResults()) {
            ItemStack secondaryStack = itemFactory.create(secondaryResult).createItemStack();
            location.getWorld().dropItemNaturally(location, secondaryStack);
        }

        // Play completion effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f).play(location);

        // Reset state
        hammerSwings = 0;
        currentRecipe = null;
        
        // Check for new recipe with remaining items
        resetAndCheckRecipe();
        
        // Update progress display
        updateHammerProgressDisplay();
    }

    /**
     * Gets the number of items currently on the anvil.
     * @return The item count
     */
    public int getItemCount() {
        return anvilItems.getContent().size();
    }

    public boolean isFull() {
        return getItemCount() >= anvilItems.maxSize();
    }

    /**
     * Checks if the anvil has any items.
     * @return true if the anvil has items
     */
    public boolean hasItems() {
        return !anvilItems.getContent().isEmpty();
    }

    /**
     * Gets the progress percentage for the current recipe.
     * @return Progress as a float between 0.0 and 1.0, or 0.0 if no recipe
     */
    public float getProgress() {
        if (currentRecipe == null) {
            return 0.0f;
        }
        return Math.min(1.0f, (float) hammerSwings / currentRecipe.getRequiredHammerSwings());
    }

    /**
     * Sets the anvil location for display entity management.
     * @param location The location of the anvil
     */
    public void setAnvilLocation(@NotNull Location location) {
        this.anvilLocation = location.clone();
        // Create the hammer progress display
        createHammerProgressDisplay();
    }
    
    /**
     * Gets the anvil location.
     * @return The anvil location, or null if not set
     */
    public Location getAnvilLocation() {
        return anvilLocation;
    }
    
    /**
     * Creates display entities for an item on the anvil.
     * @param item The item to display
     * @param itemIndex The index of the item in the anvil
     */
    private void createDisplayEntityForItem(@NotNull ItemInstance item, int itemIndex) {
        if (anvilLocation == null) {
            return;
        }
        
        ItemStack displayStack = item.createItemStack();
        boolean isStackable = displayStack.getMaxStackSize() > 1 && displayStack.getAmount() > 1;
        int entityCount = isStackable ? Math.min(3, displayStack.getAmount()) : 1;
        List<ItemDisplay> entities = new ArrayList<>();

        // Calculate position for this entity, random offset
        Location entityLocation = anvilLocation.clone();
        entityLocation.add(0.0, DISPLAY_HEIGHT_OFFSET + Math.random() * 0.05, 0.0);
        final double randX = Math.cos(Math.toRadians(Math.random() * 90)) * (Math.random() - 0.5);
        final double randZ = Math.sin(Math.toRadians(Math.random() * 90)) * (Math.random() - 0.5);
        entityLocation.add(
                randX,
                0.0,
                randZ
        );

        final float angle = (float) Math.random() * 360f;
        float scale = 0.7f;

        for (int i = 0; i < entityCount; i++) {
            final Location displayLoc = entityLocation.clone();
            displayLoc.add(0.0, i * STACK_HEIGHT_OFFSET, 0.0); // Stack items vertically
            
            // Spawn the item display entity
            ItemDisplay displayEntity = anvilLocation.getWorld().spawn(displayLoc, ItemDisplay.class);
            displayEntity.setItemStack(displayStack);
            displayEntity.setPersistent(false);
            displayEntity.setBillboard(Display.Billboard.FIXED);
            
            // Set transformation to lay the item down
            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0), // Translation
                new AxisAngle4f(angle, 0, 0, 1), // Rotation (laying down with slight variation)
                new Vector3f(scale), // Scale (smaller)
                new AxisAngle4f(0, 0, 0, 0) // Left rotation
            );
            displayEntity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            displayEntity.setTransformation(transformation);
            
            entities.add(displayEntity);
            scale -= 0.01f; // Slightly decrease scale for stacked items, Z-fighting prevention
        }
        
        // Ensure we have enough space in the list
        while (itemDisplayEntities.size() <= itemIndex) {
            itemDisplayEntities.add(new ArrayList<>());
        }
        
        itemDisplayEntities.set(itemIndex, entities);
    }
    
    /**
     * Removes the display entity for the last item.
     */
    private void removeLastDisplayEntity() {
        if (itemDisplayEntities.isEmpty()) {
            return;
        }
        
        List<ItemDisplay> lastEntities = itemDisplayEntities.removeLast();
        for (ItemDisplay entity : lastEntities) {
            entity.remove();
        }
    }
    
         /**
      * Refreshes all display entities to match current items.
      */
     public void refreshDisplayEntities() {
         // Remove all existing display entities
         clearAllDisplayEntities();
         
         // Recreate display entities for current items
         List<ItemInstance> currentItems = anvilItems.getContent();
         for (int i = 0; i < currentItems.size(); i++) {
             createDisplayEntityForItem(currentItems.get(i), i);
         }
     }
     
     /**
      * Updates display entities after recipe execution, preserving unchanged items.
      */
     private void updateDisplayEntitiesAfterRecipe() {
         if (anvilLocation == null) {
             return;
         }
         
         List<ItemInstance> currentItems = anvilItems.getContent();
         
         // Remove display entities for slots that no longer have items
         for (int i = currentItems.size(); i < itemDisplayEntities.size(); i++) {
             if (itemDisplayEntities.get(i) != null) {
                 for (ItemDisplay entity : itemDisplayEntities.get(i)) {
                     entity.remove();
                 }
             }
         }
         
         // Resize the display entities list to match current items
         while (itemDisplayEntities.size() > currentItems.size()) {
             itemDisplayEntities.removeLast();
         }
         
         // Update existing slots and create new ones
         for (int i = 0; i < currentItems.size(); i++) {
             ItemInstance item = currentItems.get(i);
             
             // Ensure we have this slot in our display entities list
             while (itemDisplayEntities.size() <= i) {
                 itemDisplayEntities.add(new ArrayList<>());
             }
             
             updateDisplayEntityForSlot(i, item);
         }
     }
     
     /**
      * Updates display entities for a specific slot, handling stack size changes.
      */
     private void updateDisplayEntityForSlot(int slotIndex, @NotNull ItemInstance item) {
         List<ItemDisplay> currentEntities = itemDisplayEntities.get(slotIndex);
         ItemStack displayStack = item.createItemStack();
         boolean isStackable = displayStack.getMaxStackSize() > 1 && displayStack.getAmount() > 1;
         int requiredEntityCount = isStackable ? 3 : 1;
         
         // If the number of required entities changed, recreate them
         if (currentEntities.size() != requiredEntityCount) {
             // Remove old entities
             for (ItemDisplay entity : currentEntities) {
                 entity.remove();
             }
             currentEntities.clear();
             
             // Create new entities with correct count
             createDisplayEntityForItem(item, slotIndex);
         } else {
             // Update existing entities with new item stack
             for (ItemDisplay entity : currentEntities) {
                 entity.setItemStack(displayStack);
             }
         }
     }
    
    /**
     * Clears all display entities.
     */
    private void clearAllDisplayEntities() {
        for (List<ItemDisplay> entities : itemDisplayEntities) {
            for (ItemDisplay entity : entities) {
                entity.remove();
            }
        }
        itemDisplayEntities.clear();
    }
    
    /**
     * Creates the hammer progress text display.
     */
    private void createHammerProgressDisplay() {
        if (anvilLocation == null) {
            return;
        }
        
        // Remove existing display if it exists
        removeHammerProgressDisplay();
        
        // Create new text display
        Location textLocation = anvilLocation.clone();
        textLocation.add(0.0, TEXT_DISPLAY_HEIGHT_OFFSET, 0.0);
        
        hammerProgressDisplay = anvilLocation.getWorld().spawn(textLocation, TextDisplay.class);
        hammerProgressDisplay.setPersistent(false);
        hammerProgressDisplay.setBillboard(Display.Billboard.CENTER);
        hammerProgressDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Transparent background
        
        // Initially hide the display (no recipe)
        updateHammerProgressDisplay();
    }
    
    /**
     * Updates the hammer progress text display.
     */
    private void updateHammerProgressDisplay() {
        if (hammerProgressDisplay == null) {
            return;
        }
        
        if (currentRecipe == null || !hasItems()) {
            // Hide the display when no recipe or no items
            hammerProgressDisplay.text(Component.empty());
            return;
        }
        
        int required = currentRecipe.getRequiredHammerSwings();
        int remaining = required - hammerSwings;
        
        // Create progress text with color coding
        Component progressText = Component.empty();
        if (remaining > 0) {
            // Show remaining swings
            final TextColor color = ProgressColor.of((float) hammerSwings / required).getTextColor();
            progressText = Component.text(remaining, color);
        }
        hammerProgressDisplay.text(progressText);
    }

    /**
     * Removes the hammer progress text display.
     */
    private void removeHammerProgressDisplay() {
        if (hammerProgressDisplay != null) {
            hammerProgressDisplay.remove();
            hammerProgressDisplay = null;
        }
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        // Clear display entities
        clearAllDisplayEntities();
        
        // Remove progress display
        removeHammerProgressDisplay();
        
        // Drop all items from the anvil
        anvilItems.onRemoval(instance, cause);
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        // Clear display entities
        clearAllDisplayEntities();
        
        // Remove progress display
        removeHammerProgressDisplay();
        
        if (anvilItems instanceof UnloadHandler unloadHandler) {
            unloadHandler.onUnload(instance);
        }
    }
} 