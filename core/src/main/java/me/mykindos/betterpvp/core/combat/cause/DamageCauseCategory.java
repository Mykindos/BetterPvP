package me.mykindos.betterpvp.core.combat.cause;

/**
 * Categories for organizing damage causes
 */
public enum DamageCauseCategory {
    /**
     * Melee attacks, sword strikes, etc.
     */
    MELEE,
    
    /**
     * Arrows, thrown items, etc
     */
    RANGED,

    /**
     * Potion effects
     */
    MAGIC,
    
    /**
     * Environmental damage like lava, fall damage, etc.
     */
    ENVIRONMENTAL,
    
    /**
     * Champion skills, item abilities, etc.
     */
    ABILITY,
    
    /**
     * Any other damage cause
     */
    OTHER
}
