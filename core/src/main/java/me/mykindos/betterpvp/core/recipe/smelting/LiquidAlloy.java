package me.mykindos.betterpvp.core.recipe.smelting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a liquid alloy result from smelting, measured in millibuckets (mB).
 * Contains the alloy type and the quantity of liquid metal produced.
 */
@Getter
public class LiquidAlloy {
    
    private final @NotNull Alloy alloyType;
    private int millibuckets;

    public LiquidAlloy(@NotNull Alloy alloyType, int millibuckets) {
        if (millibuckets <= 0) {
            throw new IllegalArgumentException("Millibuckets cannot be zero or negative: " + millibuckets);
        }
        this.alloyType = alloyType;
        this.millibuckets = millibuckets;
    }

    /**
     * Gets the name of this liquid alloy.
     * @return The alloy name
     */
    public @NotNull String getName() {
        return "Molten " + alloyType.getName();
    }
    
    @Override
    public String toString() {
        return millibuckets + "mB of " + getName();
    }

    public void add(@NotNull LiquidAlloy other) {
        if (other.alloyType != this.alloyType) {
            throw new IllegalArgumentException("Cannot add different alloy types: " + this.alloyType + " and " + other.alloyType);
        }
        add(other.millibuckets);
    }

    public void add(int millibuckets) {
        if (millibuckets < 0) {
            throw new IllegalArgumentException("Cannot add negative millibuckets: " + millibuckets);
        }
        this.millibuckets += millibuckets;
    }
} 