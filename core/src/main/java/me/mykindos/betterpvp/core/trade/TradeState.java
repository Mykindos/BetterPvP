package me.mykindos.betterpvp.core.trade;

public enum TradeState {

    /** Both sides can change their offer. Accepting here only records intent. */
    NEGOTIATING,

    /**
     * Both sides have accepted and the offers are frozen. Either player may still back out until the
     * countdown elapses; nothing has moved yet.
     */
    ACKNOWLEDGING,

    /** The swap has happened. Terminal. */
    SETTLED,

    /** The trade ended without a swap; escrowed items have gone back to their owners. Terminal. */
    CANCELLED
}
