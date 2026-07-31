package me.mykindos.betterpvp.core.trade;

/**
 * Why a trade ended without settling. Every one of these returns both escrows untouched.
 */
public enum TradeCancelReason {

    /** One of the players pressed cancel or deny. */
    CANCELLED,

    /** The negotiation ran past its time limit. */
    TIMED_OUT,

    /** A player logged out or was otherwise no longer available. */
    DISCONNECTED,

    /** A player died mid-trade. */
    DIED,

    /** A player closed the trade window. */
    CLOSED,

    /**
     * Settlement re-checked the offers and found them no longer payable - a balance dropped between
     * accepting and settling. Nothing moves.
     */
    INSUFFICIENT_FUNDS,

    /** A player did not have the inventory space to receive what they were owed. */
    NO_SPACE
}
