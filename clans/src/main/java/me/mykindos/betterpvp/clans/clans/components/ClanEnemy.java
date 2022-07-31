package me.mykindos.betterpvp.clans.clans.components;

import lombok.Data;

@Data
public class ClanEnemy {

    private final String otherClan;
    private int dominance;

    public ClanEnemy(String otherClan, int dominance) {
        this.otherClan = otherClan;
        this.dominance = dominance;
    }

}
