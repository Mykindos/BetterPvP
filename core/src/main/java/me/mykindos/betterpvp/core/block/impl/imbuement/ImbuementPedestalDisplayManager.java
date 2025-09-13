package me.mykindos.betterpvp.core.block.impl.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.EXPANSION_DURATION_MS;
import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.EXPANSION_SCALE;
import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.FLYING_HEIGHT_MIN;
import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.GROUPING_HEIGHT;
import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.HELIX_AMPLITUDE;
import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.TEXT_DISPLAY_HEIGHT_OFFSET;

/**
 * Manages all display entities and animations for ImbuementPedestal
 */
@Getter
public class ImbuementPedestalDisplayManager {

    private final List<FlyingItemData> flyingItems = new ArrayList<>();
    private Location pedestalLocation = null;
    private TextDisplay recipeReadyDisplay = null;

    /**
     * Creates a flying item display for the given item.
     */
    public void createFlyingItemDisplay(@NotNull ItemInstance item) {
        if (pedestalLocation == null) {
            return;
        }

        ItemStack displayStack = item.createItemStack();
        Location spawnLocation = pedestalLocation.clone().add(0, FLYING_HEIGHT_MIN, 0);

        // Spawn the item display entity
        ItemDisplay displayEntity = pedestalLocation.getWorld().spawn(spawnLocation, ItemDisplay.class);
        displayEntity.setItemStack(displayStack);
        displayEntity.setPersistent(false);
        displayEntity.setBillboard(Display.Billboard.FIXED);
        displayEntity.setInterpolationDuration(2);
        displayEntity.setTeleportDuration(2);

        // Set initial transformation
        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0), // Translation
                new AxisAngle4f(0, 0, 1, 0), // Rotation
                new Vector3f(0.7f), // Scale (smaller than normal)
                new AxisAngle4f(0, 0, 0, 0) // Left rotation
        );
        displayEntity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        displayEntity.setTransformation(transformation);

        // Create flying item data and add to list
        FlyingItemData flyingData = new FlyingItemData(displayEntity, item);
        flyingItems.add(flyingData);
    }

    /**
     * Removes the last flying item display.
     */
    public void removeLastFlyingItem() {
        if (flyingItems.isEmpty()) {
            return;
        }

        FlyingItemData lastItem = flyingItems.removeLast();
        lastItem.getItemDisplay().remove();
    }

    /**
     * Clears all flying item displays.
     */
    public void clearAllFlyingItems() {
        for (FlyingItemData flyingData : flyingItems) {
            flyingData.getItemDisplay().remove();
        }
        flyingItems.clear();
    }

    /**
     * Updates the recipe ready text display.
     */
    public void updateRecipeReadyDisplay(boolean hasRecipe, boolean isExecutingRecipe, boolean hasItems) {
        if (pedestalLocation == null) {
            return;
        }

        if (hasRecipe && !isExecutingRecipe && hasItems) {
            // Show recipe ready message
            if (recipeReadyDisplay == null) {
                createRecipeReadyDisplay();
            }

            recipeReadyDisplay.text(Component.text("Right-click to imbue", NamedTextColor.LIGHT_PURPLE));
        } else {
            // Hide display
            hideRecipeReadyDisplay();
        }
    }

    /**
     * Creates the recipe ready text display.
     */
    private void createRecipeReadyDisplay() {
        if (pedestalLocation == null || recipeReadyDisplay != null) {
            return;
        }

        Location textLocation = pedestalLocation.clone();
        textLocation.add(0.0, TEXT_DISPLAY_HEIGHT_OFFSET, 0.0);

        recipeReadyDisplay = pedestalLocation.getWorld().spawn(textLocation, TextDisplay.class);
        recipeReadyDisplay.setPersistent(false);
        recipeReadyDisplay.setBillboard(Display.Billboard.CENTER);
        recipeReadyDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Transparent background
    }

    /**
     * Hides the recipe ready display.
     */
    public void hideRecipeReadyDisplay() {
        if (recipeReadyDisplay != null) {
            recipeReadyDisplay.remove();
            recipeReadyDisplay = null;
        }
    }

    /**
     * Updates flying item positions and particles for normal animation.
     */
    public void updateFlyingItems() {
        if (pedestalLocation == null) {
            return;
        }

        for (FlyingItemData flyingData : flyingItems) {
            // Update angle for circular motion
            flyingData.setAngle(flyingData.getAngle() + flyingData.getAngularSpeed());

            // Update radius fluctuation offset
            flyingData.setRadiusOffset(flyingData.getRadiusOffset() + flyingData.getRadiusFluctuationSpeed());

            // Update item rotation
            flyingData.setItemRotationX(flyingData.getItemRotationX() + flyingData.getItemRotationSpeedX());
            flyingData.setItemRotationY(flyingData.getItemRotationY() + flyingData.getItemRotationSpeedY());
            flyingData.setItemRotationZ(flyingData.getItemRotationZ() + flyingData.getItemRotationSpeedZ());

            // Calculate helix height using sine wave with offset
            double helixHeight = flyingData.getBaseHeight() +
                    Math.sin(flyingData.getAngle() * 2 + flyingData.getHelixOffset()) * HELIX_AMPLITUDE;

            // Calculate new position with fluctuating radius
            double currentRadius = flyingData.getCurrentRadius();
            double x = Math.cos(flyingData.getAngle()) * currentRadius;
            double z = Math.sin(flyingData.getAngle()) * currentRadius;
            Location newLocation = pedestalLocation.clone().add(x, helixHeight, z);

            // Update item display position
            flyingData.getItemDisplay().teleport(newLocation);

            // Update item display rotation transformation
            Transformation currentTransform = flyingData.getItemDisplay().getTransformation();
            Transformation newTransform = new Transformation(
                    currentTransform.getTranslation(),
                    new AxisAngle4f((float) flyingData.getItemRotationX(), 1, 0, 0), // X rotation
                    currentTransform.getScale(),
                    new AxisAngle4f((float) flyingData.getItemRotationY(), 0, 1, 0)  // Y rotation (left rotation)
            );
            flyingData.getItemDisplay().setTransformation(newTransform);

            Particle.DUST.builder()
                    .location(newLocation)
                    .count(1)
                    .extra(0.5)
                    .data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 0.6f))
                    .receivers(60)
                    .spawn();
        }
    }

    /**
     * Moves items progressively toward the center.
     * @return true if all items have reached the center, false otherwise
     */
    public boolean moveItemsToCenter(long elapsed) {
        if (pedestalLocation == null) {
            return true;
        }

        Location groupCenter = pedestalLocation.clone().add(0, GROUPING_HEIGHT, 0);
        boolean allItemsAtCenter = true;

        for (FlyingItemData flyingData : flyingItems) {
            Location currentPos = flyingData.getItemDisplay().getLocation();
            
            // Check if item is already at the center (within a small tolerance)
            if (currentPos.distanceSquared(groupCenter) < 0.1 * 0.1) {
                // Play sound ONCE
                if (!flyingData.getItemDisplay().isSilent()) {
                    new SoundEffect(Sound.BLOCK_POLISHED_TUFF_BREAK, 0.5f, 0.4f).play(pedestalLocation);
                }
                flyingData.getItemDisplay().setSilent(true);
                // End sound

                // Item is at center, just show particles
                Particle.DUST.builder()
                        .location(groupCenter)
                        .count(1)
                        .extra(0.5)
                        .data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 0.6f))
                        .receivers(60)
                        .spawn();
                continue;
            }

            // Move item progressively toward center
            Vector direction = groupCenter.toVector().subtract(currentPos.toVector());
            Location newPos = currentPos.clone().add(direction.multiply(flyingData.getRadiusFluctuationSpeed() / 1.5)); // Smooth movement

            flyingData.getItemDisplay().teleport(newPos);

            // Item is not at center yet
            allItemsAtCenter = false;

            // Show particles during movement
            Particle.DUST.builder()
                    .location(newPos)
                    .count(1)
                    .extra(0.5)
                    .data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 0.6f))
                    .receivers(60)
                    .spawn();
        }

        if (!allItemsAtCenter) {
            Particle.OMINOUS_SPAWNING.builder()
                    .location(pedestalLocation.clone().add(0, GROUPING_HEIGHT, 0))
                    .count(7)
                    .extra(1)
                    .receivers(60)
                    .spawn();
        }

        return allItemsAtCenter;
    }

    /**
     * Updates the expansion phase animation.
     */
    public void updateExpansionPhase(long elapsed) {
        if (pedestalLocation == null) {
            return;
        }

        float progress = Math.min(1.0f, (float) elapsed / EXPANSION_DURATION_MS);
        float currentScale = 1.0f + (EXPANSION_SCALE - 1.0f) * progress;

        for (FlyingItemData flyingData : flyingItems) {
            // Update scale while preserving rotation
            Vector3f newScale = flyingData.getOriginalScale().mul(currentScale, new Vector3f());
            Transformation newTransform = new Transformation(
                    flyingData.getItemDisplay().getTransformation().getTranslation(),
                    new AxisAngle4f((float) flyingData.getItemRotationX(), 1, 0, 0), // Preserve X rotation
                    newScale,
                    new AxisAngle4f((float) flyingData.getItemRotationY(), 0, 1, 0)  // Preserve Y rotation
            );
            flyingData.getItemDisplay().setTransformation(newTransform);

            // Show expansion particles
            if (Bukkit.getCurrentTick() % 2 == 0) {
                Particle.END_ROD.builder()
                        .location(flyingData.getItemDisplay().getLocation())
                        .count(1)
                        .extra(0.05)
                        .receivers(60)
                        .spawn();
            }
        }
    }

    /**
     * Refreshes all display entities by recreating them.
     */
    public void refreshDisplayEntities(@NotNull List<ItemInstance> items) {
        // Clear existing displays
        clearAllFlyingItems();
        hideRecipeReadyDisplay();

        // Recreate displays for current items
        for (ItemInstance item : items) {
            if (item != null) {
                createFlyingItemDisplay(item);
            }
        }
    }

    /**
     * Cleans up all display entities.
     */
    public void cleanup() {
        clearAllFlyingItems();
        hideRecipeReadyDisplay();
    }

    @Nullable
    public Location getPedestalLocation() {
        return pedestalLocation != null ? pedestalLocation.clone() : null;
    }

    /**
     * Sets the pedestal location and initializes displays
     */
    public void setPedestalLocation(@NotNull Location location) {
        this.pedestalLocation = location.clone();
    }
} 