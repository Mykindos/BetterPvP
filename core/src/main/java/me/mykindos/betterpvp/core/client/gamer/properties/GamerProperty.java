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

    /**
     * Represents the amount of times a gamer has died
     * <p>Casting class {@link Integer}</p>
     */
    DEATHS,

    TIME_PLAYED,
    REMAINING_PVP_PROTECTION,

    /**
     * Incremented whenever a non player living entity is killed by a gamer
     * <p>Casting class {@link Integer}</p>
     */
    MOB_KILLS,

}
