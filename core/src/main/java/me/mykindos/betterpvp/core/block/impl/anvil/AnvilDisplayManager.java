package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.BASE_ITEM_SCALE;
import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.DISPLAY_HEIGHT_OFFSET;
import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.MAX_STACK_DISPLAY_COUNT;
import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.SCALE_REDUCTION_PER_STACK;
import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.STACK_HEIGHT_OFFSET;
import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.TEXT_DISPLAY_HEIGHT_OFFSET;

/**
 * Manages all display entities and visual effects for Anvil
 */
@Getter
public class AnvilDisplayManager {

    private final List<List<ItemDisplay>> itemDisplayEntities = new ArrayList<>();
    private Location anvilLocation = null;
    private TextDisplay hammerProgressDisplay = null;

    /**
     * Creates display entities for an item on the anvil.
     *
     * @param item      The item to display
     * @param itemIndex The index of the item in the anvil
     */
    public void createDisplayEntityForItem(@NotNull ItemInstance item, int itemIndex) {
        if (anvilLocation == null) {
            return;
        }

        ItemStack displayStack = item.createItemStack();
        boolean isStackable = displayStack.getMaxStackSize() > 1 && displayStack.getAmount() > 1;
        int entityCount = isStackable ? Math.min(MAX_STACK_DISPLAY_COUNT, displayStack.getAmount()) : 1;
        List<ItemDisplay> entities = new ArrayList<>();

        // Calculate position for this entity, random offset
        Location entityLocation = anvilLocation.clone();
        entityLocation.add(0.0, DISPLAY_HEIGHT_OFFSET + Math.random() * 0.05, 0.0);
        final double randX = Math.cos(Math.toRadians(Math.random() * 90)) * (Math.random() - 0.5);
        final double randZ = Math.sin(Math.toRadians(Math.random() * 90)) * (Math.random() - 0.5);
        entityLocation.add(randX, 0.0, randZ);

        final float angle = (float) Math.random() * 360f;
        float scale = BASE_ITEM_SCALE;

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
            scale -= SCALE_REDUCTION_PER_STACK; // Slightly decrease scale for stacked items, Z-fighting prevention
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
    public void removeLastDisplayEntity() {
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
    public void refreshDisplayEntities(@NotNull List<ItemInstance> items) {
        // Remove all existing display entities
        clearAllDisplayEntities();

        // Recreate display entities for current items
        for (int i = 0; i < items.size(); i++) {
            createDisplayEntityForItem(items.get(i), i);
        }
    }

    /**
     * Updates display entities after recipe execution, preserving unchanged items.
     */
    public void updateDisplayEntitiesAfterRecipe(@NotNull List<ItemInstance> currentItems) {
        if (anvilLocation == null) {
            return;
        }

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
        int requiredEntityCount = isStackable ? MAX_STACK_DISPLAY_COUNT : 1;

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
     * Clears all item display entities.
     */
    public void clearAllDisplayEntities() {
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
    }

    /**
     * Updates the hammer progress text display.
     */
    public void updateHammerProgressDisplay(boolean hasRecipe, boolean hasItems, int hammerSwings, int requiredSwings) {
        if (hammerProgressDisplay == null) {
            return;
        }

        if (!hasRecipe || !hasItems) {
            // Hide the display when no recipe or no items
            hammerProgressDisplay.text(Component.empty());
            return;
        }

        int remaining = requiredSwings - hammerSwings;

        // Create progress text with color coding
        Component progressText = Component.empty();
        if (remaining > 0) {
            // Show remaining swings
            final TextColor color = ProgressColor.of((float) hammerSwings / requiredSwings).getTextColor();
            progressText = Component.text(remaining, color);
        }
        hammerProgressDisplay.text(progressText);
    }

    /**
     * Removes the hammer progress text display.
     */
    public void removeHammerProgressDisplay() {
        if (hammerProgressDisplay != null) {
            hammerProgressDisplay.remove();
            hammerProgressDisplay = null;
        }
    }

    /**
     * Cleans up all display entities.
     */
    public void cleanup() {
        clearAllDisplayEntities();
        removeHammerProgressDisplay();
    }

    @Nullable
    public Location getAnvilLocation() {
        return anvilLocation != null ? anvilLocation.clone() : null;
    }

    /**
     * Sets the anvil location and initializes displays.
     */
    public void setAnvilLocation(@NotNull Location location) {
        this.anvilLocation = location.clone();
        createHammerProgressDisplay();
    }
} 