package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.Getter;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages liquid storage and capacity for Smelter
 */
@Getter
public class SmelterLiquidManager {

    private final int maxLiquidCapacity; // Maximum liquid capacity in millibuckets
    @Nullable
    private LiquidAlloy storedLiquid; // The liquid alloy stored in this smelter

    public SmelterLiquidManager(int maxLiquidCapacity) {
        this.maxLiquidCapacity = maxLiquidCapacity;
        this.storedLiquid = null;
    }

    /**
     * Gets the current liquid capacity usage in millibuckets.
     *
     * @return Current liquid amount, or 0 if no liquid is stored
     */
    public int getCurrentLiquidAmount() {
        return storedLiquid != null ? storedLiquid.getMillibuckets() : 0;
    }

    /**
     * Gets the remaining liquid capacity in millibuckets.
     *
     * @return Remaining capacity
     */
    public int getRemainingLiquidCapacity() {
        return maxLiquidCapacity - getCurrentLiquidAmount();
    }

    /**
     * Checks if the smelter has enough capacity for the given amount of liquid.
     *
     * @param millibuckets Amount to check
     * @return true if there's enough capacity
     */
    public boolean hasCapacityFor(int millibuckets) {
        return getRemainingLiquidCapacity() >= millibuckets;
    }

    /**
     * Checks if the given liquid alloy is compatible with the stored liquid.
     *
     * @param newLiquid The liquid to check compatibility with
     * @return true if compatible (same alloy type or no stored liquid)
     */
    public boolean isCompatibleWith(@NotNull LiquidAlloy newLiquid) {
        if (storedLiquid == null) {
            return true; // No stored liquid, so any liquid is compatible
        }
        return storedLiquid.getAlloyType().equals(newLiquid.getAlloyType());
    }

    /**
     * Checks if we can add the given liquid (both compatibility and capacity).
     *
     * @param newLiquid The liquid to check
     * @return true if the liquid can be added
     */
    public boolean canAddLiquid(@NotNull LiquidAlloy newLiquid) {
        return isCompatibleWith(newLiquid) && hasCapacityFor(newLiquid.getMillibuckets());
    }

    /**
     * Adds liquid to the storage.
     *
     * @param newLiquid The liquid to add
     * @return true if the liquid was added successfully
     */
    public boolean addLiquid(@NotNull LiquidAlloy newLiquid) {
        if (!canAddLiquid(newLiquid)) {
            return false;
        }

        if (storedLiquid == null) {
            storedLiquid = new LiquidAlloy(newLiquid.getAlloyType(), newLiquid.getMillibuckets());
        } else {
            storedLiquid.add(newLiquid);
        }

        return true;
    }

    /**
     * Consumes the specified amount of liquid.
     *
     * @param millibuckets Amount to consume
     * @return true if the liquid was consumed
     */
    public boolean consumeLiquid(int millibuckets) {
        if (storedLiquid == null || storedLiquid.getMillibuckets() < millibuckets) {
            return false;
        }

        int newAmount = storedLiquid.getMillibuckets() - millibuckets;
        if (newAmount <= 0) {
            storedLiquid = null; // Remove empty liquid
        } else {
            storedLiquid = new LiquidAlloy(storedLiquid.getAlloyType(), newAmount);
        }

        return true;
    }

    /**
     * Checks if there's enough liquid of the specified amount.
     *
     * @param millibuckets Amount to check
     * @return true if there's enough liquid
     */
    public boolean hasLiquid(int millibuckets) {
        return storedLiquid != null && storedLiquid.getMillibuckets() >= millibuckets;
    }

    /**
     * Checks if any liquid is stored.
     *
     * @return true if liquid is stored
     */
    public boolean hasLiquid() {
        return storedLiquid != null && storedLiquid.getMillibuckets() > 0;
    }

    /**
     * Clears all stored liquid.
     */
    public void clearLiquid() {
        storedLiquid = null;
    }

    /**
     * Sets the stored liquid directly (for serialization/deserialization).
     *
     * @param liquid The liquid to set
     */
    public void setStoredLiquid(@Nullable LiquidAlloy liquid) {
        this.storedLiquid = liquid;
    }
} 