package me.mykindos.betterpvp.core.block.impl.smelter;

import com.google.common.base.Preconditions;
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
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    private LiquidAlloy smeltingAlloy;
    @Nullable
    private CastingMold castingMold; // The casting mold used for casting
    @Nullable
    private CastingMoldRecipe currentCastingRecipe; // The current casting mold recipe being used
    @Nullable
    private SmeltingRecipe currentSmeltingRecipe;
    private long lastCast;
    private float smeltingProgress = 0.0f;

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

    private long getCastTime() {
        return 2_000L; // 2 seconds per cast
    }

    /**
     * Gets the smelting time in milliseconds. This represents the time required to complete one smelting cycle.
     * @return Smelting time in milliseconds
     */
    public long getSmeltingTime() {
        return 4_000L; // 4 seconds per cycle
    }

    public boolean matchRecipe() {
        currentSmeltingRecipe = null;

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
        Optional<SmeltingRecipe> recipeOpt = smeltingService.findMatchingRecipe(itemMap);
        if (recipeOpt.isEmpty()) {
            smeltingProgress = 0.0f;
            smeltingAlloy = null;
            currentSmeltingRecipe = null;
            return false; // No matching recipe
        }

        currentSmeltingRecipe = recipeOpt.get();
        return true;
    }

    /**
     * Processes smelting if conditions are met.
     *
     * @param liquidManager The liquid manager to store results
     * @param fuelManager   The fuel manager to check temperature
     * @return true if smelting was processed
     */
    public boolean processSmelting(@NotNull SmelterLiquidManager liquidManager, SmelterFuelManager fuelManager) {
        if (currentSmeltingRecipe == null || !fuelManager.isBurning()) {
            smeltingProgress = 0.0f;
            return false;
        }

        // Convert content to ItemInstance map for recipe execution
        List<ItemInstance> content = contentItems.getContent();
        Map<Integer, ItemInstance> itemInstanceMap = new HashMap<>();
        for (int i = 0; i < content.size(); i++) {
            ItemInstance item = content.get(i);
            itemInstanceMap.put(i, item);
        }

        if (fuelManager.getTemperature() < currentSmeltingRecipe.getMinimumTemperature()) {
            smeltingProgress = 0.0f;
            return false;
        }

        // Execute the recipe to get the result
        SmeltingResult smeltingResult = smeltingService.executeRecipe(currentSmeltingRecipe, itemInstanceMap);
        LiquidAlloy resultLiquid = smeltingResult.getPrimaryResult();

        // Check if we can add the liquid
        if (!liquidManager.canAddLiquid(resultLiquid)) {
            smeltingProgress = 0.0f;
            return false; // Cannot add liquid (incompatible or not enough capacity)
        }

        // If the last smelting alloy is different, reset the smelting progress with this new one
        if (resultLiquid != smeltingAlloy) {
            smeltingProgress = 0.0f;
            smeltingAlloy = resultLiquid;
            return false;
        }

        // Increment smelting progress
        if (smeltingProgress < 1.0f) {
            smeltingProgress += 1.0f / 20L / (getSmeltingTime() / 1000f); // This is called every tick
        } else {
            // Update content items after consumption
            contentItems.setContent(new ArrayList<>(itemInstanceMap.values()));

            // Store the liquid alloy
            liquidManager.addLiquid(resultLiquid);
            smeltingProgress = 0.0f; // Start counting down again
        }
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
        currentCastingRecipe = null; // Reset current recipe for casting mold

        if (!liquidManager.hasLiquid()) {
            return false; // No registry or no liquid to work with
        }

        if (castingMold == null) {
            return false; // No casting mold in slot
        }

        // Find a recipe for this casting mold
        Optional<CastingMoldRecipe> recipeOpt = castingMoldRecipeRegistry.findRecipeForAlloy(
                castingMold, Objects.requireNonNull(liquidManager.getStoredLiquid()).getAlloyType()
        );

        if (recipeOpt.isEmpty()) {
            return false; // No recipe found for this mold and alloy combination
        }

        CastingMoldRecipe recipe = recipeOpt.get();
        currentCastingRecipe = recipe; // Set the current recipe for GUI updates

        // Check if we have enough liquid
        if (!liquidManager.hasLiquid(recipe.getRequiredMillibuckets())) {
            return false; // Not enough liquid
        }

        // Check if the alloy type is compatible
        if (!recipe.acceptsAlloy(liquidManager.getStoredLiquid().getAlloyType())) {
            return false;
        }

        if (UtilTime.elapsed(lastCast, getCastTime())) {
            // Get the result item
            // Check if we can add the result
            ItemInstance result = recipe.createPrimaryResult();
            if (!canAddResultItem(result)) {
                return false; // Cannot add result item
            }

            // Add the result item
            addResultItem(result);
            lastCast = System.currentTimeMillis();

            // Consume the liquid
            liquidManager.consumeLiquid(recipe.getRequiredMillibuckets());

            // Play sound effect
            new SoundEffect(Sound.BLOCK_LAVA_EXTINGUISH, CASTING_VOLUME, CASTING_PITCH).play(instance.getLocation());
        }

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

        if (currentResults.stream().noneMatch(Objects::isNull)) {
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

        if (currentResults.stream().noneMatch(Objects::nonNull) || currentResults.getFirst() == null) {
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

    public float getSmeltingProgress() {
        return Math.max(0.0f, Math.min(1.0f, smeltingProgress));
    }
}