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

    public void addDominance(int dominance) {
        this.dominance = Math.min(100, this.dominance + dominance);
    }

    public void takeDominance(int dominance) {
        this.dominance = Math.max(0, this.dominance - dominance);
    }


}
