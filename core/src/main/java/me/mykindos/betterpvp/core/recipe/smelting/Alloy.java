package me.mykindos.betterpvp.core.recipe.smelting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all alloy types in the smelting system.
 * Each concrete alloy implementation should extend this class and provide
 * the specific properties for that alloy type.
 */
@Getter
@RequiredArgsConstructor
public abstract class Alloy {
    
    private final @NotNull String name;
    private final @NotNull String textureKey;
    private final @NotNull BaseItem ingotItem;
    private final @NotNull Color color;
    private final float minimumTemperature;
    
    /**
     * Gets the display name for this alloy.
     * @return The alloy name
     */
    public @NotNull String getName() {
        return name;
    }
    
    /**
     * Gets the base item that represents the solid ingot form of this alloy.
     * @return The base item for the ingot
     */
    public @NotNull BaseItem getIngotItem() {
        return ingotItem;
    }
    
    /**
     * Gets the minimum temperature required to smelt this alloy.
     * @return The minimum temperature in Celsius
     */
    public float getMinimumTemperature() {
        return minimumTemperature;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Alloy alloy = (Alloy) obj;
        return name.equals(alloy.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
} 