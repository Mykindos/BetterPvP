package me.mykindos.betterpvp.core.world.zone;

/**
 * The kind of action being evaluated against a {@link Zone}'s rules. Consumers (clans, mini-games, environmental
 * features) branch on this in their {@link ZoneRule}s and {@link ZoneInteractEvent} handlers.
 */
public enum ZoneInteraction {

    /** A block is being broken. */
    BREAK,
    /** A block is being placed. */
    PLACE,
    /** A block or entity is being interacted with (right-click). */
    INTERACT,
    /** Damage is being dealt or received. */
    DAMAGE,
    /** An item or container is being used. */
    USE,
    /** Anything not covered above. */
    OTHER
}
