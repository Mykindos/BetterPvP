package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;

@Data
public class FlashData {

    private int charges = 0;

    public void useCharge(){
        charges = Math.max(0, charges - 1);
    }

    public void addCharge(){
        charges++;
    }

}
