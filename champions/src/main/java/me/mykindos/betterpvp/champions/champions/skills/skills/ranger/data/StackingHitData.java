package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Data;

@Data
public class StackingHitData {

    private int charge;
    private long lastHit = System.currentTimeMillis();

    public void addCharge(){
        charge++;
        lastHit = System.currentTimeMillis();
    }

}
