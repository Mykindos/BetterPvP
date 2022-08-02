package me.mykindos.betterpvp.clans.clans.components;

import lombok.Data;
import me.mykindos.betterpvp.clans.clans.Clan;

@Data
public class ClanEnemy {

    private final Clan clan;
    private int dominance;

    public ClanEnemy(Clan clan, int dominance) {
        this.clan = clan;
        this.dominance = dominance;
    }

}
