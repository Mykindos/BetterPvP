package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Data;

@Data
public class StampedeData {

    private long sprintTime;

    private int sprintStrength;

    public StampedeData(long sprintTime, int sprintStrength) {
        this.sprintTime = sprintTime;
        this.sprintStrength = sprintStrength;
    }
}
