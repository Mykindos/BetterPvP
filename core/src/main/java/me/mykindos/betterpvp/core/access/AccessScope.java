package me.mykindos.betterpvp.core.access;

/**
 * Defines the scopes that item access can be gated on.
 */
public enum AccessScope {
    /** Crafting the item at a crafting table. */
    CRAFT,
    /** Using the item's interactions (right-click, left-click abilities). */
    USE,
    /** Dealing damage with the item as a weapon. */
    DAMAGE,
    /** Wearing the item in an armour slot. */
    WEAR
}
