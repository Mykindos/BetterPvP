package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;

@Data
public class FlashData {

    int charges = 0;

    public void addCharge(){
        charges++;
    }

}
