package me.mykindos.betterpvp.core.loot;

/**
 * Represents the strategy to distribute loot weights across a table.
 */
public enum WeightDistributionStrategy {

    /**
     * The loot weights are distributed uniformly. They are not modified.
     */
    STATIC,

    /**
     * The loot weights are distributed uniformly, but pity rules are respected
     * to shift individual weights towards the center.
     */
    PITY,

    /**
     * All weights are shifted towards the center.
     */
    PROGRESSIVE;

}
