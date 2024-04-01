package me.mykindos.betterpvp.clans.logging.types;

public enum ClanLogType {
    /**
     * A player1 joins a Clan1
     */
    CLAN_JOIN,
    /**
     * A player1 leaves a Clan1
     */
    CLAN_LEAVE,
    /**
     * A player1 is kicked by player2 from a Clan1/2
     */
    CLAN_KICK,
    /**
     *
     */
    CLAN_CREATE,
    /**
     *
     */
    CLAN_DISBAND,
    /**
     * Represents a clan kill. Not used to put data in the database, only on retrieval
     */
    CLAN_KILL,
    /**
     * Player 1 claims territory for Clan2
     */
    CLAN_CLAIM,
    /**
     * Player1 of Clan1 unclaims territory from Clan2
     */
    CLAN_UNCLAIM,
    /**
     * Player1 of Clan1 invites Player2
     */
    CLAN_INVITE,
    /**
     * Player1 of Clan1 requests an alliance with Clan2
     */
    CLAN_ALLIANCE_REQUEST,
    /**
     * Player1 of Clan1 accepts an alliance with Clan2
     */
    CLAN_ALLIANCE_ACCEPT,
    /**
     * Player1 of Clan1 removes an alliance with Clan2
     */
    CLAN_ALLIANCE_REMOVE,
    /**
     * Player1 of Clan1 requests a trust with Clan2
     */
    CLAN_TRUST_REQUEST,
    /**
     * Player1 of Clan1 accepts a trust with Clan2
     */
    CLAN_TRUST_ACCEPT,
    /**
     * Player1 of Clan1 removes a trust with Clan2
     */
    CLAN_TRUST_REMOVE,
    /**
     * Player1 of Clan1 requests neutral with Clan2
     */
    CLAN_NEUTRAL_REQUEST,
    /**
     * Player1 of Clan1 accepts neutral with Clan2
     */
    CLAN_NEUTRAL_ACCEPT,
    /**
     * Player1 of Clan1 enemies Clan2
     */
    CLAN_ENEMY,
    /**
     * Player 1 of Clan1 sets their home
     */
    CLAN_SETHOME,
    /**
     * Player1 of Clan1 is promoted by Player2 of Clan2
     */
    CLAN_PROMOTE,
    /**
     * Player1 of Clan1 is demoted by Player2 of Clan2
     */

    //TODO more enums

}
