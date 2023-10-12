package me.mykindos.betterpvp.core.components.clans.data;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;

@Data
public class ClanEnemy {

    private final IClan clan;
    private double dominance;

    public ClanEnemy(IClan clan, double dominance) {
        this.clan = clan;
        this.dominance = dominance;
    }

    public void addDominance(double dominance) {
        this.dominance = Math.min(100, this.dominance + dominance);
    }

    public void takeDominance(double dominance) {
        this.dominance = Math.max(0, this.dominance - dominance);
    }


}
