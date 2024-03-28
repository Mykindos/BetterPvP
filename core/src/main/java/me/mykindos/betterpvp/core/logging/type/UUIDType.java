package me.mykindos.betterpvp.core.logging.type;

public enum UUIDType {
    /**
     * Represents the primary player, generally the player doing the action
     */
    PLAYER1,
    /**
     * Represents a secondary player, general the player the action occurs to
     */
    PLAYER2,
    /**
     * The clan of PLAYER1
     */
    CLAN1,
    /**
     * The clan of PLAYER2
     */
    CLAN2,
    ITEM,
    NONE
}