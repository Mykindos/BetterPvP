package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.BURN_TIME_DECREASE_PER_TICK;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.DEFAULT_FUEL_SLOTS;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.FUEL_CONSUME_PITCH;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.FUEL_CONSUME_VOLUME;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.TEMPERATURE_DECREASE_PER_TICK;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.TEMPERATURE_INCREASE_PER_TICK;

/**
 * Manages fuel consumption, burn time, and temperature for Smelter
 */
@Getter
@Setter
public class SmelterFuelManager {

    private final ItemFactory itemFactory;
    private final long maxBurnTime;
    private final StorageBlockData fuelItems;

    private long burnTime = 0L; // Millis
    private float temperature = 0.0f; // Celsius

    public SmelterFuelManager(@NotNull ItemFactory itemFactory, long maxBurnTime) {
        this.itemFactory = itemFactory;
        this.maxBurnTime = maxBurnTime;
        this.fuelItems = new StorageBlockData(DEFAULT_FUEL_SLOTS);
    }

    /**
     * Checks if the smelter is currently burning.
     *
     * @return true if burning
     */
    public boolean isBurning() {
        return burnTime > 0;
    }

    /**
     * Updates the burn time and temperature each tick.
     */
    public void updatePerTick() {
        // Decrease burn time
        if (burnTime > 0) {
            setBurnTime(Math.max(0, getBurnTime() - BURN_TIME_DECREASE_PER_TICK));
        }

        // Handle temperature
        if (burnTime > 0) {
            // Increase temperature while burning
            increaseFuelTemperature();
        } else {
            // Decrease temperature when not burning
            setTemperature(Math.max(0, getTemperature() - TEMPERATURE_DECREASE_PER_TICK));
        }
    }

    /**
     * Tries to consume fuel if needed and possible.
     *
     * @param location Location for sound effects
     * @return true if fuel was consumed
     */
    public boolean tryConsumeFuel(@NotNull Location location) {
        final Optional<FuelComponent> fuelComponentOpt = getFuel();
        if (fuelComponentOpt.isEmpty()) {
            return false; // No fuel available
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

            // Add burn time
            setBurnTime(burnTime + fuelBurnTime);

            // Play sound effect
            new SoundEffect(Sound.BLOCK_LAVA_EXTINGUISH, FUEL_CONSUME_VOLUME, FUEL_CONSUME_PITCH).play(location);

            return true;
        }

        return false;
    }

    /**
     * Gets the current fuel component if available.
     *
     * @return Optional fuel component
     */
    private Optional<FuelComponent> getFuel() {
        List<ItemInstance> currentFuel = fuelItems.getContent();
        if (currentFuel.isEmpty()) {
            return Optional.empty(); // No fuel available
        }

        ItemInstance fuelItem = currentFuel.getFirst();
        return fuelItem.getComponent(FuelComponent.class);
    }

    /**
     * Increases temperature towards the fuel's target temperature.
     */
    private void increaseFuelTemperature() {
        Optional<FuelComponent> fuelComponentOpt = getFuel();
        if (fuelComponentOpt.isEmpty()) {
            return; // No fuel available
        }

        FuelComponent fuelComponent = fuelComponentOpt.get();
        float targetTemperature = fuelComponent.getMaxTemperature();

        // Gradually increase temperature towards the fuel's max temperature
        if (temperature < targetTemperature) {
            setTemperature(Math.min(targetTemperature, temperature + TEMPERATURE_INCREASE_PER_TICK));
        }
    }

    /**
     * Checks if the smelter has sufficient temperature for operations.
     *
     * @return true if temperature is above 0
     */
    public boolean hasHeat() {
        return temperature > 0;
    }
} 