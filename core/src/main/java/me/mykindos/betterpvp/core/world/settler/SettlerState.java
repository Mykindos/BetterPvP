package me.mykindos.betterpvp.core.world.settler;

/** What a settler is doing right now. */
public enum SettlerState {
    /** Has no place to work, or needs none. */
    IDLE,
    /** Assigned to a workplace. */
    WORKING,
    /** Refuses to work until paid. */
    STRIKING
}
