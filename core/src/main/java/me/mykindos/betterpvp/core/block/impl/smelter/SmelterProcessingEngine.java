package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingResult;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.CASTING_PITCH;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.CASTING_VOLUME;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.DEFAULT_CONTENT_SLOTS;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.DEFAULT_RESULT_SLOTS;

/**
 * Handles smelting operations and casting mold processing for Smelter
 */
@Getter
@Setter
public class SmelterProcessingEngine {

    private final SmeltingService smeltingService;
    private final ItemFactory itemFactory;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;

    private final StorageBlockData contentItems;
    private final StorageBlockData resultItems;
    private final StorageBlockData castingMoldItems;

    @Nullable
    private CastingMold castingMold; // The casting mold used for casting
    @Nullable
    private CastingMoldRecipe currentRecipe; // The current casting mold recipe being used

    public SmelterProcessingEngine(@NotNull SmeltingService smeltingService,
                                   @NotNull ItemFactory itemFactory,
                                   @NotNull CastingMoldRecipeRegistry castingMoldRecipeRegistry) {
        this.smeltingService = smeltingService;
        this.itemFactory = itemFactory;
        this.castingMoldRecipeRegistry = castingMoldRecipeRegistry;

        this.contentItems = new StorageBlockData(DEFAULT_CONTENT_SLOTS);
        this.resultItems = new StorageBlockData(DEFAULT_RESULT_SLOTS);
        this.castingMoldItems = new StorageBlockData();
    }

    /**
     * Processes smelting if conditions are met.
     *
     * @param liquidManager The liquid manager to store results
     * @param temperature   Current temperature
     * @return true if smelting was processed
     */
    public boolean processSmelting(@NotNull SmelterLiquidManager liquidManager, float temperature) {
        if (smeltingService == null || itemFactory == null) {
            return false; // Dependencies not available
        }

        // Convert content items to the format expected by SmeltingService
        Map<Integer, ItemStack> itemMap = new HashMap<>();
        List<ItemInstance> content = contentItems.getContent();
        for (int i = 0; i < content.size(); i++) {
            ItemInstance item = content.get(i);
            if (item != null) {
                itemMap.put(i, item.createItemStack());
            }
        }

        // Check if we have a matching recipe
        Optional<SmeltingRecipe> recipeOpt = smeltingService.findMatchingRecipe(itemMap, temperature);
        if (recipeOpt.isEmpty()) {
            return false; // No matching recipe
        }

        SmeltingRecipe recipe = recipeOpt.get();

        // Convert content to ItemInstance map for recipe execution
        Map<Integer, ItemInstance> itemInstanceMap = new HashMap<>();
        for (int i = 0; i < content.size(); i++) {
            ItemInstance item = content.get(i);
            if (item != null) {
                itemInstanceMap.put(i, item);
            }
        }

        // Execute the recipe to get the result
        SmeltingResult smeltingResult = smeltingService.executeRecipe(recipe, itemInstanceMap);
        LiquidAlloy resultLiquid = smeltingResult.getPrimaryResult();

        // Check if we can add the liquid
        if (!liquidManager.canAddLiquid(resultLiquid)) {
            return false; // Cannot add liquid (incompatible or not enough capacity)
        }

        // Update content items after consumption
        List<ItemInstance> newContent = itemInstanceMap.values().stream()
                .filter(Objects::nonNull)
                .toList();
        contentItems.setContent(newContent);

        // Store the liquid alloy
        liquidManager.addLiquid(resultLiquid);

        return true;
    }

    /**
     * Tries to fill casting molds with liquid.
     *
     * @param liquidManager The liquid manager to consume from
     * @param instance      The smelter instance for sound effects
     * @return true if casting was processed
     */
    public boolean tryFillCastingMold(@NotNull SmelterLiquidManager liquidManager,
                                      @NotNull SmartBlockInstance instance) {
        currentRecipe = null; // Reset current recipe for casting mold

        if (castingMoldRecipeRegistry == null || !liquidManager.hasLiquid()) {
            return false; // No registry or no liquid to work with
        }

        if (castingMold == null) {
            return false; // No casting mold in slot
        }

        // Find a recipe for this casting mold
        Optional<CastingMoldRecipe> recipeOpt = castingMoldRecipeRegistry.findRecipeForAlloy(
                castingMold, liquidManager.getStoredLiquid().getAlloyType()
        );

        if (recipeOpt.isEmpty()) {
            return false; // No recipe found for this mold and alloy combination
        }

        CastingMoldRecipe recipe = recipeOpt.get();
        currentRecipe = recipe; // Set the current recipe for GUI updates

        // Check if we have enough liquid
        if (!liquidManager.hasLiquid(recipe.getRequiredMillibuckets())) {
            return false; // Not enough liquid
        }

        // Get the result item
        Optional<ItemInstance> resultOpt = recipe.getResult(liquidManager.getStoredLiquid().getAlloyType())
                .map(itemFactory::create);

        if (resultOpt.isEmpty()) {
            return false; // Could not create result item
        }

        // Check if we can add the result
        ItemInstance result = resultOpt.get();
        if (!canAddResultItem(result)) {
            return false; // Cannot add result item
        }

        // Add the result item
        addResultItem(result);

        // Consume the liquid
        liquidManager.consumeLiquid(recipe.getRequiredMillibuckets());

        // Play sound effect
        new SoundEffect(Sound.BLOCK_LAVA_EXTINGUISH, CASTING_VOLUME, CASTING_PITCH).play(instance.getLocation());

        return true;
    }

    /**
     * Checks if a result item can be added to the result storage.
     *
     * @param result The item to check
     * @return true if the item can be added
     */
    private boolean canAddResultItem(@NotNull ItemInstance result) {
        List<ItemInstance> currentResults = resultItems.getContent();

        if (currentResults.isEmpty()) {
            return true; // Empty, can add
        }

        ItemInstance current = currentResults.getFirst();
        if (current == null) {
            return true; // Null current, can add
        }

        ItemStack first = current.createItemStack();
        ItemStack second = result.createItemStack();

        if (!first.isSimilar(second)) {
            return false; // Different items, cannot combine
        }

        return first.getAmount() + second.getAmount() <= first.getType().getMaxStackSize();
    }

    /**
     * Adds a result item to the result storage.
     *
     * @param result The item to add
     */
    private void addResultItem(@NotNull ItemInstance result) {
        List<ItemInstance> currentResults = resultItems.getContent();

        if (currentResults.isEmpty() || currentResults.getFirst() == null) {
            // Replace with new item
            resultItems.setContent(List.of(result));
        } else {
            // Combine with existing item
            ItemInstance current = currentResults.getFirst();
            ItemStack first = current.createItemStack();
            ItemStack second = result.createItemStack();

            first.setAmount(first.getAmount() + second.getAmount());
            Optional<ItemInstance> combinedOpt = itemFactory.fromItemStack(first);
            if (combinedOpt.isPresent()) {
                resultItems.setContent(List.of(combinedOpt.get()));
            }
        }
    }
} 