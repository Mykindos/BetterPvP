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
     * A smelting recipe that combines metals to form alloys in liquid form.
     */
    SMELTING,
    
    /**
     * A recipe that requires items to be hammered on an anvil for a specific number of swings.
     */
    ANVIL_CRAFTING,
    
    /**
     * A recipe for other game systems like enchanting, brewing, etc.
     */
    CUSTOM
} 