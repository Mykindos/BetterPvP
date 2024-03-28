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
    CLAN_KILL
    //TODO more enums

}
