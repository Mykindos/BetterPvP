package me.mykindos.betterpvp.core.combat.modifiers;

/**
 * Represents how a modifier value should be applied.
 */
public enum DamageOperator {

    /**
     * Adds or subtracts a flat value
     */
    FLAT,

    /**
     * Multiplies the damage by a factor,
     * additively in the case of increase and multiplicatively in the case of decrease
     */
    MULTIPLIER

}
