package me.mykindos.betterpvp.core.combat.damage;

/**
 * Represents how a modifier value should be applied.
 */
public enum ModifierValue {
    /**
     * Adds or subtracts a flat value
     */
    FLAT,
    
    /**
     * Adds or subtracts a percentage of the current value
     * For example, a value of 30 would add 30% to the current value
     */
    PERCENTAGE
}