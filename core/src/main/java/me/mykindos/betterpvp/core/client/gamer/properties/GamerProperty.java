package me.mykindos.betterpvp.core.client.gamer.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum GamerProperty {

    // Currency
    BALANCE,
    FRAGMENTS,

    // Blocks
    BLOCKS_PLACED,
    BLOCKS_BROKEN,

    // Damage
    DAMAGE_DEALT,
    DAMAGE_TAKEN,

    TIME_PLAYED,
    REMAINING_PVP_PROTECTION,

}
