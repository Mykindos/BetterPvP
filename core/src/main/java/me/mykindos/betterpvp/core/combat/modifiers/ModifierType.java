package me.mykindos.betterpvp.core.combat.modifiers;

/**
 * Types of damage modifiers for categorization and exclusion
 */
public enum ModifierType {
    /**
     * Generic modifiers (flat increases, percentage increases, etc.)
     */
    GENERIC,
    
    /**
     * Environmental modifiers (weather, location-based effects, etc.)
     */
    ENVIRONMENTAL,
    
    /**
     * Skill-based modifiers (champion skills, clan abilities, etc.)
     */
    ABILITY,
    
    /**
     * Weapon-based modifiers (custom weapons, enchantments, etc.)
     */
    WEAPON,

    /**
     * Item statistics
     */
    STAT,

    /**
     * Armor
     */
    ARMOR,

    /**
     * Potion effect modifiers (strength, resistance, etc.)
     */
    EFFECT,
    
    /**
     * Any other type of modifier
     */
    OTHER
}
