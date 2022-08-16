package me.mykindos.betterpvp.core.gamer.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum GamerProperty {

    // Misc
    SIDEBAR_ENABLED,

    // Chat modes
    CLAN_CHAT,
    ALLY_CHAT,
    STAFF_CHAT,

    // Currency
    COINS,
    FRAGMENTS,

    // Blocks
    BLOCKS_PLACED,
    BLOCKS_BROKEN,

    // Damage
    DAMAGE_DEALT,
    DAMAGE_TAKEN


}
