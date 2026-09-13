package me.mykindos.betterpvp.core.world.site.crew;

/**
 * What happened when somebody tried to join a crew.
 * <p>
 * Returned rather than signalled with a boolean because every one of these needs a different thing said to the player —
 * "they have to let you aboard" and "there is no room" are both refusals, and telling them apart is the difference
 * between waiting and finding another ship.
 */
public enum JoinOutcome {

    /** Straight in — a clanmate or ally, who never has to ask. */
    JOINED,

    /** Added to the captain's queue; they decide. */
    REQUESTED,

    /** Already asked this captain and the request is still standing. */
    ALREADY_REQUESTED,

    /** Already aboard this crew. */
    ALREADY_MEMBER,

    /** The hull is at capacity. */
    CREW_FULL,

    /** Captains cannot join other crews — leaving the ship is how you stop being one. */
    IS_CAPTAIN,

    /** The crew has already departed. */
    SAILING
}
