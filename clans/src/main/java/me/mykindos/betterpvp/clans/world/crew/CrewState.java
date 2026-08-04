package me.mykindos.betterpvp.clans.world.crew;

/**
 * Whether a crew is still forming or already at sea.
 * <p>
 * The distinction is what governs every membership rule: at the dock a crew is an open thing people join and leave by
 * walking on and off the hull, and once it sails it is a fixed list. Nothing about a voyage can be undone, so nothing
 * about the crew changes during one.
 */
public enum CrewState {

    /** At the dock, gathering. Requests are taken, members come and go with the hull bounds. */
    MUSTERING,

    /** Under way. The roster is frozen: no joins, no requests, and leaving the hull means nothing. */
    SAILING
}
