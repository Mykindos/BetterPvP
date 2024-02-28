package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import lombok.Data;

@Data
public class WreathData {

    int charges = 0;

    public void addCharge(){
        charges++;
    }

}
