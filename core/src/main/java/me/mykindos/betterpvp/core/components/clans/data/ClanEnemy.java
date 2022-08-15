package me.mykindos.betterpvp.core.components.clans.data;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;

@Data
public class ClanEnemy {

    private final IClan clan;
    private int dominance;

    public ClanEnemy(IClan clan, int dominance) {
        this.clan = clan;
        this.dominance = dominance;
    }

}
