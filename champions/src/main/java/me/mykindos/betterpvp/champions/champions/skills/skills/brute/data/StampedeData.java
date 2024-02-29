package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class StampedeData {
    @Getter @Setter
    private long sprintTime;
    @Getter @Setter
    private int sprintStrength;

    public StampedeData(long sprintTime, int sprintStrength) {
        this.sprintTime = sprintTime;
        this.sprintStrength = sprintStrength;
    }
}
