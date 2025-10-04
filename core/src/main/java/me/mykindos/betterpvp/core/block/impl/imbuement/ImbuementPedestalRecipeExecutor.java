package me.mykindos.betterpvp.core.block.impl.imbuement;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeResult;
import me.mykindos.betterpvp.core.imbuement.RuneImbuementRecipe;
import me.mykindos.betterpvp.core.imbuement.StandardImbuementRecipe;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.mykindos.betterpvp.core.block.impl.imbuement.ImbuementPedestalConstants.*;

/**
 * Handles recipe execution, timing, and effects for ImbuementPedestal
 */
@Getter
@Setter
public class ImbuementPedestalRecipeExecutor {

    private final ItemFactory itemFactory;
    private final ImbuementPedestalDisplayManager displayManager;

    private boolean isExecutingRecipe = false;
    private long recipeStartTime = 0L;
    private int recipePhase = 0; // 0: moving to center, 1: expansion, 2: failure explosion
    private boolean recipeFailed = false;

    public ImbuementPedestalRecipeExecutor(@NotNull ItemFactory itemFactory,
                                           @NotNull ImbuementPedestalDisplayManager displayManager) {
        this.itemFactory = itemFactory;
        this.displayManager = displayManager;
    }

    /**
     * Executes the given recipe with a chance of failure.
     *
     * @param recipe The recipe to execute
     * @param player The player executing the recipe
     * @return true if recipe execution started, false otherwise
     */
    public boolean executeRecipe(@NotNull ImbuementRecipe recipe, @NotNull Player player) {
        Preconditions.checkState(!isExecutingRecipe, "Recipe is already executing");
        Preconditions.checkState(displayManager.getPedestalLocation() != null, "Pedestal location is not set");

        // Start recipe execution
        isExecutingRecipe = true;
        recipeStartTime = System.currentTimeMillis();
        recipePhase = 0;
        recipeFailed = false;

        // Hide recipe ready display during execution
        displayManager.hideRecipeReadyDisplay();

        // Play start sound
        new SoundEffect(Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f).play(displayManager.getPedestalLocation());

        return true;
    }

    /**
     * Handles recipe failure with effects.
     */
    private void executeFailure() {
        Location pedestalLocation = displayManager.getPedestalLocation();
        if (pedestalLocation == null) {
            return;
        }

        // Remove all flying items
        displayManager.clearAllFlyingItems();

        // Play failure effects
        new SoundEffect(Sound.BLOCK_GLASS_BREAK, 0.7f, 0.2f).play(pedestalLocation);

        // Small explosion particles
        Particle.REVERSE_PORTAL.builder()
                .location(pedestalLocation.clone().add(0, 1.5, 0))
                .count(30)
                .receivers(60)
                .spawn();

        // Reset state
        isExecutingRecipe = false;
    }

    /**
     * Updates recipe execution animation phases.
     *
     * @return true if recipe execution is complete, false if still in progress
     */
    public boolean updateRecipeExecution() {
        if (!isExecutingRecipe) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - recipeStartTime;

        switch (recipePhase) {
            case 0: // Moving items to center
                if (displayManager.moveItemsToCenter(elapsed)) {
                    // All items have reached the center, start expansion phase
                    recipePhase = 1;
                    recipeStartTime = System.currentTimeMillis(); // Reset timer for expansion phase
                }
                break;

            case 1: // Expansion phase
                // Check for failure at the start of expansion phase
                if (!recipeFailed && Math.random() < FAILURE_CHANCE) {
                    recipeFailed = true;
                }
                
                displayManager.updateExpansionPhase(elapsed);
                if (elapsed >= EXPANSION_DURATION_MS) {
                    if (recipeFailed) {
                        // Recipe failed during expansion, show failure explosion
                        recipePhase = 2;
                        recipeStartTime = System.currentTimeMillis(); // Reset timer for failure explosion
                    } else {
                        // Recipe succeeded, complete normally
                        return true; // Recipe execution complete
                    }
                }
                recipeFailed = false;
                break;

            case 2: // Failure explosion phase
                // Show failure explosion for a short duration
                if (elapsed >= 500) { // 0.5 seconds of failure explosion
                    executeFailure();
                    return true; // Recipe execution complete (with failure)
                }
                break;
        }

        return false;
    }

    /**
     * Completes the recipe execution with effects and item drops.
     */
    public void completeRecipe(@NotNull ImbuementRecipe recipe, @NotNull List<ItemInstance> currentItems) {
        Location pedestalLocation = displayManager.getPedestalLocation();
        if (pedestalLocation == null) {
            return;
        }

        // Play explosion effect
        Location explosionCenter = pedestalLocation.clone().add(0, GROUPING_HEIGHT, 0);
        Particle.END_ROD.builder()
                .location(explosionCenter)
                .extra(0.2)
                .count(50)
                .receivers(60)
                .spawn();
        Particle.FLASH.builder()
                .location(explosionCenter)
                .receivers(60)
                .spawn();

        // Play completion sound
        new SoundEffect(Sound.ITEM_TOTEM_USE, 1.5f, 0.4f).play(pedestalLocation);

        // Drop the primary result towards the nearest player
        ItemStack primaryResult = createPrimaryResult(recipe, currentItems);
        explosionCenter.getWorld().dropItem(explosionCenter, primaryResult, item -> {
            Player nearestPlayer = findNearestPlayer(pedestalLocation);
            if (nearestPlayer != null) {
                Vector direction = nearestPlayer.getLocation().toVector().subtract(explosionCenter.toVector()).normalize();
                direction.multiply(0.3); // Gentle throw
                direction.add(new Vector(0, 0.3, 0));
                item.setVelocity(direction);
            }
        });

        // Drop secondary results (only for standard recipes)
        if (recipe instanceof StandardImbuementRecipe standardRecipe) {
            ImbuementRecipeResult result = standardRecipe.getPrimaryResult();
            for (var secondaryResult : result.getSecondaryResults()) {
                ItemStack secondaryStack = itemFactory.create(secondaryResult).createItemStack();
                explosionCenter.getWorld().dropItemNaturally(explosionCenter, secondaryStack);
            }
        }

        // Clear all items and reset state
        displayManager.clearAllFlyingItems();
        isExecutingRecipe = false;
    }

    /**
     * Creates the primary result item for the recipe.
     */
    private ItemStack createPrimaryResult(@NotNull ImbuementRecipe recipe, @NotNull List<ItemInstance> currentItems) {
        if (recipe instanceof RuneImbuementRecipe runeRecipe) {
            // For rune recipes, we need to pass the current items
            return runeRecipe.createPrimaryResult(currentItems).createItemStack();
        } else {
            // For standard recipes, use the normal method
            return recipe.createPrimaryResult().createItemStack();
        }
    }

    /**
     * Finds the nearest player to the pedestal.
     */
    @Nullable
    private Player findNearestPlayer(@NotNull Location pedestalLocation) {
        return pedestalLocation.getNearbyPlayers(10.0)
                .stream()
                .min((p1, p2) -> Double.compare(
                        p1.getLocation().distanceSquared(pedestalLocation),
                        p2.getLocation().distanceSquared(pedestalLocation)
                ))
                .orElse(null);
    }

    /**
     * Resets the executor state.
     */
    public void reset() {
        isExecutingRecipe = false;
        recipeStartTime = 0L;
        recipePhase = 0;
    }
} 