package me.mykindos.betterpvp.core.logging.type;

public enum UUIDType {
    /**
     * Represents the primary player, generally the player doing the action
     */
    MAINPLAYER,
    /**
     * Represents a secondary player, general the player the action occurs to
     */
    OTHERPLAYER,
    /**
     * The clan of PLAYER1
     */
    MAINCLAN,
    /**
     * The clan of PLAYER2
     */
    OTHERCLAN,
    ITEM,
    NONE
}