package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.TickHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Main coordinator class for Smelter functionality.
 * Delegates responsibilities to specialized components.
 */
@RequiredArgsConstructor
@Getter
@Setter
public class SmelterData implements RemovalHandler, LoadHandler, TickHandler {

    private final SmeltingService smeltingService;
    private final ItemFactory itemFactory;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;
    private final long maxBurnTime;
    private final int maxLiquidCapacity; // Maximum liquid capacity in millibuckets

    // Component managers
    private final SmelterFuelManager fuelManager;
    private final SmelterLiquidManager liquidManager;
    private final SmelterProcessingEngine processingEngine;
    private final SmelterEffectsManager effectsManager;

    @Setter(AccessLevel.NONE)
    private GuiSmelter gui;

    // Constructor for dependency injection
    public SmelterData(@NotNull SmeltingService smeltingService,
                       @NotNull ItemFactory itemFactory,
                       @NotNull CastingMoldRecipeRegistry castingMoldRecipeRegistry,
                       long maxBurnTime,
                       int maxLiquidCapacity) {
        this.smeltingService = smeltingService;
        this.itemFactory = itemFactory;
        this.castingMoldRecipeRegistry = castingMoldRecipeRegistry;
        this.maxBurnTime = maxBurnTime;
        this.maxLiquidCapacity = maxLiquidCapacity;

        // Initialize component managers
        this.fuelManager = new SmelterFuelManager(itemFactory, maxBurnTime);
        this.liquidManager = new SmelterLiquidManager(maxLiquidCapacity);
        this.processingEngine = new SmelterProcessingEngine(smeltingService, itemFactory, castingMoldRecipeRegistry);
        this.effectsManager = new SmelterEffectsManager();
    }

    // Delegate methods for backward compatibility and convenience
    public boolean isBurning() {
        return fuelManager.isBurning();
    }

    public int getCurrentLiquidAmount() {
        return liquidManager.getCurrentLiquidAmount();
    }

    public int getRemainingLiquidCapacity() {
        return liquidManager.getRemainingLiquidCapacity();
    }

    public boolean hasCapacityFor(int millibuckets) {
        return liquidManager.hasCapacityFor(millibuckets);
    }

    public long getBurnTime() {
        return fuelManager.getBurnTime();
    }

    public void setBurnTime(long burnTime) {
        fuelManager.setBurnTime(burnTime);
    }

    public float getTemperature() {
        return fuelManager.getTemperature();
    }

    public void setTemperature(float temperature) {
        fuelManager.setTemperature(temperature);
    }

    // Storage accessors (for GUI and serialization)
    public SmelterProcessingEngine getProcessingEngine() {
        return processingEngine;
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        if (gui != null) {
            gui.closeForAllViewers();
        }
        // Drop items from all inventories by delegating to their removal handlers
        processingEngine.getContentItems().onRemoval(instance, cause);
        fuelManager.getFuelItems().onRemoval(instance, cause);
        processingEngine.getResultItems().onRemoval(instance, cause);
    }

    @Override
    public void onTick(@NotNull SmartBlockInstance instance) {
        // Skip processing if dependencies are not available
        if (smeltingService == null || itemFactory == null) {
            return;
        }

        // Update fuel and temperature
        fuelManager.updatePerTick();

        // Try to consume fuel if needed
        if (fuelManager.tryConsumeFuel(instance.getLocation())) {
            // Sync storage to update GUI if fuel was consumed
            if (gui != null) {
                gui.syncFromStorage();
            }
        }

        // Process smelting if we have heat and valid items
        if (fuelManager.hasHeat()) {
            if (processingEngine.processSmelting(liquidManager, fuelManager.getTemperature())) {
                // Sync GUI if smelting occurred
                if (gui != null) {
                    gui.syncFromStorage();
                }
            }
        }

        // Try to fill casting molds
        if (processingEngine.tryFillCastingMold(liquidManager, instance)) {
            // Sync GUI if casting occurred
            if (gui != null) {
                gui.syncFromStorage();
            }
        }

        // Play sound and particle effects
        effectsManager.playBurningEffects(instance.getLocation(), fuelManager.isBurning());
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        if (gui != null) {
            gui.closeForAllViewers();
        }
        // Handle unloading for storage components
        if (processingEngine.getContentItems() instanceof LoadHandler contentLoadHandler) {
            contentLoadHandler.onUnload(instance);
        }
        if (fuelManager.getFuelItems() instanceof LoadHandler fuelLoadHandler) {
            fuelLoadHandler.onUnload(instance);
        }
        if (processingEngine.getResultItems() instanceof LoadHandler resultLoadHandler) {
            resultLoadHandler.onUnload(instance);
        }
    }

    @Override
    public void onLoad(@NotNull SmartBlockInstance instance) {
        // Refresh display entities if needed
        if (processingEngine.getContentItems() instanceof LoadHandler contentLoadHandler) {
            contentLoadHandler.onLoad(instance);
        }
        if (fuelManager.getFuelItems() instanceof LoadHandler fuelLoadHandler) {
            fuelLoadHandler.onLoad(instance);
        }
        if (processingEngine.getResultItems() instanceof LoadHandler resultLoadHandler) {
            resultLoadHandler.onLoad(instance);
        }
    }

    public void openGui(@NotNull Player player, @NotNull ItemFactory itemFactory) {
        if (gui == null) {
            gui = new GuiSmelter(itemFactory, this);
            gui.syncFromStorage(); // Sync GUI with storage data
        }
        gui.show(player);
    }
}
