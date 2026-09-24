package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import lombok.Value;

/** A camp's last recorded Prosperity, and what it was at its last daily snapshot. */
@Value
public class ProsperityStanding {

    int prosperity;
    int snapshot;

    public int change() {
        return prosperity - snapshot;
    }
}
