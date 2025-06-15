package me.mykindos.betterpvp.core.recipe;

/**
 * Enum representing different types of recipes in the system.
 */
public enum RecipeType {
    
    /**
     * A crafting recipe that requires items to be placed in specific positions.
     */
    SHAPED_CRAFTING,
    
    /**
     * A crafting recipe that only requires the correct items to be present, regardless of position.
     */
    SHAPELESS_CRAFTING,
    
    /**
     * A recipe for other game systems like enchanting, brewing, etc.
     */
    CUSTOM
} 