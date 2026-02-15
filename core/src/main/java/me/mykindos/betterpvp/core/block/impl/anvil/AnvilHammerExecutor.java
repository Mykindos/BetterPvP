package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeResult;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.item.ItemStat;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.SWING_COOLDOWN_MS;

/**
 * Handles hammer swings, timing, and recipe execution for Anvil
 */
@Getter
@Setter
public class AnvilHammerExecutor {

    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private int hammerSwings = 0;
    private long lastSwingTime = 0L; // System.currentTimeMillis()

    public AnvilHammerExecutor(@NotNull ItemFactory itemFactory, ClientManager clientManager) {
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
    }

    /**
     * Checks if a player can swing the hammer (cooldown check).
     *
     * @return true if enough time has passed since the last swing
     */
    public boolean canSwing() {
        return System.currentTimeMillis() - lastSwingTime >= SWING_COOLDOWN_MS;
    }

    /**
     * Executes a hammer swing, incrementing the counter and playing effects.
     *
     * @param player   The player swinging the hammer
     * @param location The location to play effects at
     * @return true if the swing was executed (not on cooldown)
     */
    public boolean executeHammerSwing(@NotNull Player player, @NotNull Location location) {
        if (!canSwing()) {
            return false; // Cooldown not finished
        }

        lastSwingTime = System.currentTimeMillis();
        hammerSwings++;

        // Play hammer swing effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 0.7f, 0.4f).play(location);
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.2f + (float) Math.random() * 0.45f, 0.4f).play(location);
        player.swingMainHand();

        return true;
    }

    /**
     * Checks if enough swings have been completed for the given recipe.
     *
     * @param recipe The recipe to check
     * @return true if enough swings have been completed
     */
    public boolean hasEnoughSwings(@NotNull AnvilRecipe recipe) {
        return hammerSwings >= recipe.getRequiredHammerSwings();
    }

    /**
     * Executes the recipe, consuming ingredients and producing results.
     *
     * @param player  The player executing the recipe
     * @param recipe   The recipe to execute
     * @param itemsMap The items currently on the anvil
     * @param location The location to drop results at
     * @return The items remaining after recipe execution
     */
    public List<ItemInstance> executeRecipe(@NotNull Player player,
                                            @NotNull AnvilRecipe recipe,
                                            @NotNull Map<Integer, ItemInstance> itemsMap,
                                            @NotNull Location location) {

        // Consume ingredients
        recipe.consumeIngredients(itemsMap, itemFactory);

        // Get remaining items after consumption
        List<ItemInstance> remainingItems = itemsMap.values().stream()
                .filter(Objects::nonNull)
                .toList();

        // Get the recipe result
        AnvilRecipeResult result = recipe.getResult();

        //todo stats for this

        // Drop the primary result
        ItemStack primaryResult = itemFactory.create(result.getPrimaryResult()).createItemStack();
        location.getWorld().dropItemNaturally(location, primaryResult);

        final ItemStat primaryStat = ItemStat.builder()
                .itemStack(primaryResult)
                .action(ItemStat.Action.ANVIL_PRIMARY)
                .build();
        clientManager.incrementStat(player, primaryStat, 1L);

        // Drop secondary results
        for (BaseItem secondaryResult : result.getSecondaryResults()) {
            ItemStack secondaryStack = itemFactory.create(secondaryResult).createItemStack();
            location.getWorld().dropItemNaturally(location, secondaryStack);
            final ItemStat secondaryStat = ItemStat.builder()
                    .itemStack(secondaryStack)
                    .action(ItemStat.Action.ANVIL_SECONDARY)
                    .build();
            clientManager.incrementStat(player, secondaryStat, 1L);
        }

        // Play completion effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f).play(location);

        // Reset hammer swings
        reset();

        return remainingItems;
    }

    /**
     * Resets the hammer swing counter.
     */
    public void reset() {
        hammerSwings = 0;
    }

    /**
     * Gets the progress percentage for the given recipe.
     *
     * @param recipe The recipe to calculate progress for
     * @return Progress as a float between 0.0 and 1.0, or 0.0 if no recipe
     */
    public float getProgress(@NotNull AnvilRecipe recipe) {
        return Math.min(1.0f, (float) hammerSwings / recipe.getRequiredHammerSwings());
    }
} 