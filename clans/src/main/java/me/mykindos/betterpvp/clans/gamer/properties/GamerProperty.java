package me.mykindos.betterpvp.clans.gamer.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum GamerProperty {

    SIDEBAR_ENABLED("CLANS_SIDEBAR_ENABLED"),
    CLAN_CHAT("CLAN_CHAT"),
    ALLY_CHAT("ALLY_CHAT"),
    STAFF_CHAT("STAFF_CHAT"),

    COINS("COINS"),
    FRAGMENTS("FRAGMENTS"),

    BLOCKS_PLACED("BLOCKS_PLACED"),
    BLOCKS_BROKEN("BLOCKS_BROKEN");


    private final String key;


    @Override
    public String toString(){
        return key;
    }


}
