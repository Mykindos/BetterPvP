package me.mykindos.betterpvp.core.world.settler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A settler who left, kept on its roster for a while so the site can tell its owner. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlerDeparture {

    private String name;
    private SettlerRarity rarity;
    private SettlerLeaveReason reason;
    private long at;
}
