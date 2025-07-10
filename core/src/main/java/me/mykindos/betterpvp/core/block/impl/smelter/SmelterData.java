package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.TickHandler;
import me.mykindos.betterpvp.core.block.data.UnloadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingResult;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Setter
public class SmelterData implements RemovalHandler, UnloadHandler, TickHandler {

    private final SmeltingService smeltingService;
    private final ItemFactory itemFactory;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;
    private final long maxBurnTime;
    private final int maxLiquidCapacity; // Maximum liquid capacity in millibuckets
    private StorageBlockData contentItems = new StorageBlockData(10);
    private StorageBlockData resultItems = new StorageBlockData(1);
    private StorageBlockData fuelItems = new StorageBlockData(1);
    private long burnTime = 0L; // Millis
    private float temperature = 0.0f; // Celsius
    @Nullable
    private LiquidAlloy storedLiquid; // The liquid alloy stored in this smelter
    @Setter(AccessLevel.NONE)
    private GuiSmelter gui;

    public boolean isBurning() {
        return burnTime > 0;
    }

    /**
     * Gets the current liquid capacity usage in millibuckets.
     * @return Current liquid amount, or 0 if no liquid is stored
     */
    public int getCurrentLiquidAmount() {
        return storedLiquid != null ? storedLiquid.getMillibuckets() : 0;
    }

    /**
     * Gets the remaining liquid capacity in millibuckets.
     * @return Remaining capacity
     */
    public int getRemainingLiquidCapacity() {
        return maxLiquidCapacity - getCurrentLiquidAmount();
    }

    /**
     * Checks if the smelter has enough capacity for the given amount of liquid.
     * @param millibuckets Amount to check
     * @return true if there's enough capacity
     */
    public boolean hasCapacityFor(int millibuckets) {
        return getRemainingLiquidCapacity() >= millibuckets;
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        if (gui != null) {
            gui.closeForAllViewers();
        }
        // Drop items from all inventories by delegating to their removal handlers
        contentItems.onRemoval(instance, cause);
        fuelItems.onRemoval(instance, cause);
        resultItems.onRemoval(instance, cause);
    }

    @Override
    public void onTick(@NotNull SmartBlockInstance instance) {
        // Skip processing if dependencies are not available
        if (smeltingService == null || itemFactory == null) {
            return;
        }
        
        // Decrease burn time
        if (burnTime > 0) {
            setBurnTime(Math.max(0, getBurnTime() - 50)); // Decrease burn time by 50ms each tick
        }

        // Handle fuel consumption and temperature
        if (burnTime > 0) {
            // Increase temperature while burning
            increaseFuelTemperature();
        } else {
            // Decrease temperature when not burning
            setTemperature(Math.max(0, getTemperature() - 1f)); // Decrease temperature by 1 degree each tick
        }

        // Try to consume fuel if needed
        tryConsumeFuel(instance);

        // Process smelting if we have heat and valid items
        if (temperature > 0) {
            processSmelting();
        }

        // Try to fill casting molds
        tryFillCastingMold();

        // Sound cues
        if (isBurning()) {
            final int tick = Bukkit.getCurrentTick();
            if (tick % 20 == 0 && Math.random() < 0.4) {
                new SoundEffect(Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.3f).play(instance.getLocation());
            }

            if (tick % 20 == 0 & Math.random() < 0.4) {
                new SoundEffect(Sound.BLOCK_LAVA_POP, 0.4f, 0.5f).play(instance.getLocation());
            }

            if (tick % 5 == 0) {
                Particle.CAMPFIRE_COSY_SMOKE.builder()
                        .location(instance.getLocation().clone().add(0, 4.5 , 0))
                        .count(3)
                        .receivers(60)
                        .offset(0, 0.5, 0)
                        .extra(0)
                        .spawn();
            }
        }
    }

    private void tryConsumeFuel(@NotNull SmartBlockInstance instance) {
        final Optional<FuelComponent> fuelComponentOpt = getFuel();
        if (fuelComponentOpt.isEmpty()) {
            return; // No fuel available
        }

        FuelComponent fuelComponent = fuelComponentOpt.get();
        long fuelBurnTime = fuelComponent.getBurnTime();

        // Check if we should burn this fuel (only if it won't exceed max burn time)
        if (burnTime + fuelBurnTime <= maxBurnTime) {
            // Consume one fuel item
            ItemStack fuelStack = fuelItems.getContent().getFirst().createItemStack();
            if (fuelStack.getAmount() > 1) {
                fuelStack.setAmount(fuelStack.getAmount() - 1);
                Optional<ItemInstance> newFuelInstance = itemFactory.fromItemStack(fuelStack);
                if (newFuelInstance.isPresent()) {
                    fuelItems.setContent(List.of(newFuelInstance.get()));
                } else {
                    fuelItems.setContent(List.of()); // Remove invalid item
                }
            } else {
                fuelItems.setContent(List.of()); // Remove last fuel item
            }

            // Add burn time and update max temperature
            setBurnTime(burnTime + fuelBurnTime);

            // Sync storage to update GUI
            if (gui != null) {
                gui.syncFromStorage();
            }

            // Sounds cues
            new SoundEffect(Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.2f).play(instance.getLocation());
        }
    }

    private Optional<FuelComponent> getFuel() {
        List<ItemInstance> currentFuel = fuelItems.getContent();
        if (currentFuel.isEmpty()) {
            return Optional.empty(); // No fuel available
        }

        ItemInstance fuelItem = currentFuel.getFirst();
        return fuelItem.getComponent(FuelComponent.class);
    }

    private void increaseFuelTemperature() {
        Optional<FuelComponent> fuelComponentOpt = getFuel();
        if (fuelComponentOpt.isEmpty()) {
            return; // No fuel available
        }

        FuelComponent fuelComponent = fuelComponentOpt.get();
        float targetTemperature = fuelComponent.getMaxTemperature();

        // Gradually increase temperature towards the fuel's max temperature
        if (temperature < targetTemperature) {
            setTemperature(Math.min(targetTemperature, temperature + 2f)); // Increase by 2 degrees per tick
        }
    }

    private void processSmelting() {
        if (smeltingService == null || itemFactory == null) {
            return; // Dependencies not available
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
            return; // No matching recipe
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

        // Check alloy compatibility and capacity
        if (storedLiquid != null) {
            // Check if the alloy types match
            if (!storedLiquid.getAlloyType().equals(resultLiquid.getAlloyType())) {
                return; // Cannot mix different alloy types
            }
        }

        // Check if we have enough capacity for the new liquid
        if (!hasCapacityFor(resultLiquid.getMillibuckets())) {
            return; // Not enough capacity
        }

        // Update content items after consumption
        List<ItemInstance> newContent = itemInstanceMap.values().stream()
                .filter(Objects::nonNull)
                .toList();
        contentItems.setContent(newContent);

        // Store or combine the liquid alloy
        if (storedLiquid == null) {
            storedLiquid = new LiquidAlloy(resultLiquid.getAlloyType(), resultLiquid.getMillibuckets());
        } else {
            storedLiquid.add(resultLiquid);
        }

        if (gui != null) {
            gui.syncFromStorage(); // Update GUI with new content
        }
    }

    private void tryFillCastingMold() {
        if (castingMoldRecipeRegistry == null || storedLiquid == null) {
            return; // No registry or no liquid to work with
        }

        List<ItemInstance> castingMolds = resultItems.getContent();
        if (castingMolds.isEmpty()) {
            return; // No casting mold in slot
        }

        ItemInstance castingMoldItem = castingMolds.getFirst();
        
        // Find a recipe for this casting mold
        Optional<CastingMoldRecipe> recipeOpt = castingMoldRecipeRegistry.findRecipeForAlloy(
                castingMoldItem.getBaseItem(), storedLiquid.getAlloyType());
        
        if (recipeOpt.isEmpty()) {
            return; // No recipe found for this mold and alloy combination
        }

        CastingMoldRecipe recipe = recipeOpt.get();
        
        // Check if we have enough liquid
        if (storedLiquid.getMillibuckets() < recipe.getRequiredMillibuckets()) {
            return; // Not enough liquid
        }

        // Get the result item
        Optional<ItemInstance> resultOpt = recipe.getResult(storedLiquid.getAlloyType()).map(itemFactory::create);
        
        if (resultOpt.isEmpty()) {
            return; // Could not create result item
        }

        // Consume the liq uid
        int newAmount = storedLiquid.getMillibuckets() - recipe.getRequiredMillibuckets();
        if (newAmount <= 0) {
            storedLiquid = null; // Remove empty liquid
        } else {
            storedLiquid = new LiquidAlloy(storedLiquid.getAlloyType(), newAmount);
        }

        // Replace the casting mold with the filled version
        resultItems.setContent(List.of(resultOpt.get()));

        if (gui != null) {
            gui.syncFromStorage(); // Update GUI
        }
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        if (gui != null) {
            gui.closeForAllViewers();
        }
        if (contentItems instanceof UnloadHandler contentUnloadHandler) {
            contentUnloadHandler.onUnload(instance);
        }
        if (fuelItems instanceof UnloadHandler fuelUnloadHandler) {
            fuelUnloadHandler.onUnload(instance);
        }
        if (resultItems instanceof UnloadHandler resultUnloadHandler) {
            resultUnloadHandler.onUnload(instance);
        }
    }

    public void openGui(@NotNull Player player, @NotNull ItemFactory itemFactory) {
        if (gui == null) {
            gui = new GuiSmelter(itemFactory, this);
        }
        gui.show(player);
    }
}
