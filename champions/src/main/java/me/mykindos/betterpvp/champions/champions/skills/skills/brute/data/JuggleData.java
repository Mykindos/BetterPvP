package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Data;

@Data
public class JuggleData {
    private int charges = 0;

    public void addCharge() {
        charges++;
    }
}
